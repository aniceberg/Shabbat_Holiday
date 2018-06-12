/**
 *  Nite Nite
 *
 *  Copyright 2018 iceberg
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Nite Nite",
    namespace: "HomeLit",
    author: "iceberg",
    description: "Toggles lights off after bedtime and keeps them off",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Weather/weather4-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Weather/weather4-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Weather/weather4-icn@2x.png")

preferences {
	page(name: "mainPage", install: true, uninstall: true)	
}

def mainPage() {
	dynamicPage(name: "mainPage") {
	section("Bedtime", hideWhenEmpty: true) {
		input "switches", "capability.switch", title: "Which switch(es) do you want to control?", multiple: true
        input "bedtime", "time", title: "Choose a bedtime:", required: true, hideWhenEmpty: "switches"
        input "endBedtime", "time", title: "When to end bedtime rule enforcement:", required: true, hideWhenEmpty: "switches"
	}
    
    section("Bedtime message announcement", hideWhenEmpty: true) {
			input "speakers", "capability.speechSynthesis", title: "On these speakers", multiple:true, required: false
			input "speakerVolume", "number", title: "Select reminder volume", description: "0-100%", required: false, hideWhenEmpty: "speakers"
    		input "bedtimeMessage", "text", title: "Bedtime message to announce", defaultValue: "Time for bed!", required: false, hideWhenEmpty: "speakers"
            input "enforcementMessage", "bool", title: "Announce enforcement message? (e.g. 'Turning off bedroom lights in 60 seconds')", defaultValue: true, required: false, hideWhenEmpty: "speakers"
        }
    
    section("Options") {
    	input "delayTime", "number", title: "Delay shutoff time (defaults to 60 seconds)", defaultValue: "60",  range: "1..*", required: false
		input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
		input "modes", "mode", title: "Set for specific modes", multiple: true, required: false
        label(name: "label", required: false, multiple: false)
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
    schedule(bedtime, startHandler)
    schedule(endBedtime, endHandler)
	//initialize delayTime value to 60s unless another value was input
    state.delayTime = delayTime ?: 60
    state.notificationMessage = null
}

def startHandler() {
	if ( getAllOk() ) {
       	log.debug "Bedtime enforcement started at ${bedtime}."
    
    	//Queue bedtime message if set
   		if (bedtimeMessage) { composeNotification(true, null)}        
        
        //check for, queue enforcement message and turn off switches
        switchOn(null)
        
        //deliver Bedtime and/or Enforcement Message
        if (state.notificationMessage) { deliverMessage(state.notificationMessage) }
        
    	//watch for switch turned on
    	subscribe(switches, "switch.on", switchOn)
            
    	//In case the event is missed, automatically check every 10 minutes to turn off lights
    	runEvery10Minutes(switchOn(null))
        }
}

def endHandler() {
	log.debug "Bedtime enforcement ended at ${endBedtime}."
    state.notificationMessage = null
    unsubscribe()
    unschedule()
}

def switchOn(event) {
	//Log the state of the switches
	log.debug "${switches} are ${switches.currentValue('switch')}"
    
    //check switch status before sending OFF command
    switches.each {
		if ( it.currentState('switch').value.equals('on') ) {
			if (enforcementMessage) { composeNotification(null, it) }
			runIn(state.delayTime, turnOffAllSwitches)
        	}
		if ( it.currentState('switch').value.equals('off') ) {
			log.debug "The following switch is off: ${it}"
			}
		}
}

def turnOffAllSwitches() { 
    switches.off()
}

def composeNotification(bedtimeMessageFlag=null, switchName=null) {
	if (bedtimeMessageFlag) { state.notificationMessage = "${bedtimeMessage}" }   
    if (switchName) { 
        state.notificationMessage = "${state.notificationMessage} Turning off ${switchName} in ${state.delayTime} seconds!"
        }
}

def deliverMessage(msg) {
	if (speakerVolume) { speakers?.setLevel(speakerVolume) }
    speakers?.speak(msg)
}

private getAllOk() {
	modeOk && daysOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

def getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.debug "daysOk = ${result}"
	return result
}