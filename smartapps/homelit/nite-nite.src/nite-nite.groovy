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
            input "enforcementMessage", "text", title: "Enforcement message to announce. (can be left blank to disable)", defaultValue: "Turning off ${switches} in ${delayTime} seconds", required: false, hideWhenEmpty: "speakers"
        }
    
    section("Options") {
    	input "delayTime", "number", title: "Delay shutoff time (defaults to 60 second minimum)", defaultValue: "60",  range: "60..*", required: false
		input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
		input "modes", "mode", title: "Set for specific modes", multiple: true, required: false                
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
	//initialize delayTimeMS value to 60000ms unless another value was input
    state.delayTimeMS = (delayTime * 1000) ?: 60000
    log.debug "delayTimeMS = ${state.delayTimeMS}"
}

def startHandler() {
	log.debug "getDaysOk: " + getDaysOk()
	if ( getDaysOk() ) { //&& getModeOk() ) {
       	log.debug "Bedtime enforcement started at ${bedtime}."
    
    	//check for speakers and message
   		if (speakers && bedtimeMessage) { deliverMessage(bedtimeMessage) }
        
        //turn off switches
		//switches.off()
        switchOn(now)
        
    	//watch for switch turned on
    	subscribe(switches, "switch.on", switchOn)
            
    	//In case the event is missed, automatically check every 10 minutes to turn off lights
    	runEvery10Minutes(switchOn(null))
        }
}

def endHandler() {
	log.debug "Bedtime enforcement ended at ${endBedtime}."
    unsubscribe()
    unschedule()
}

def switchOn(event) {
	//Log the state of the switches
	log.debug "${switches} are ${switches.currentValue('switch')}"
    
    //check switch status before sending OFF command
    switches.each {
		if ( it.currentState('switch').value.equals('on') ) {
			log.debug "The following switch is on: " + it
			if (event.value.equals('now')) { 
            	log.debug "SwitchOn(now) event received"
                turnOffAllSwitches() 
            } else {
            	runIn(state.delayTimeMS, turnOffAllSwitches) 
            }
        	log.debug it + " turning off in " + state.delayTimeMS + "ms."
    		if (speakers && enforcementMessage) { deliverMessage(enforcementMessage) }
        	}
		if ( it.currentState('switch').value.equals('off') ) {
			log.debug "The following switch is off: " + it
			}
		}
}

def turnOffAllSwitches() { 
	switches.off()
}

def deliverMessage(msg) {
	if (speakerVolume) { speakers?.setLevel(speakerVolume) }
    log.debug "Speakers: ${speakers} enabled."
    speakers?.speak(msg)
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

def getModeOk() {
	def result = true
    //add Mode checking code
	if (mode) { }
    return result
}