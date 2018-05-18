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
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Bedtime") {
		input "bedtime", "time", title: "Choose a bedtime:", required: true
        input "endBedtime", "time", title: "When to end bedtime rules:", required: true
		input "switches", "capability.switch", title: "Which switch(es) do you want to control?", multiple: true
	}
    section("Options") {
    	input "delayTime", "number", title: "Delay shutoff time (defaults to 5 seconds)", defaultValue: "0",  range: "0..*", required: false
		input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
// Not needed because it's automatically implemented in the Mobile app? 
//		input "modes", "mode", title: "Only when mode is", multiple: true, required: false
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
	// TODO: subscribe to attributes, devices, locations, etc.
    schedule(bedtime, startHandler) //TODO: check if we're currently in enforcement
    schedule(endBedtime, endHandler)
    if (modes) {
		subscribe(location, modeChangeHandler)
	}
	//initialize delayTime value to 5000ms unless another value was input
    state.delayTime = delayTime * 1000 ?: 5000
}

// TODO: implement event handlers
def startHandler() {
	log.debug "Bedtime enforcement started at $bedtime."
   	//state.scheduledRun = true
	subscribe(switches, "switch.on", switchOn)
    switchOn(null)
}

def endHandler() {
	log.debug "Bedtime enforcement ended at $endBedtime."
    //state.scheduledRun = false
    unsubscribe()
}

def switchOn(event) {
	//log.debug "switchOn(), getAllOk: " + getAllOk()
    log.debug "switchOn(), getDaysOk: " + getDaysOk()
    //only need to check the days, not the mode?
	if ( getDaysOk() ) {
        log.debug "$switches turning off."
    	runIn(delayTime, switches.off() )
	}
}

// Commented out this section after eliminating "scheduledRun" variable and using Mobile App's native Mode Handling
//def getAllOk() {
//	return (getModeOk && getDaysOk && state.scheduledRun)
//}

// Commented out this section because using Mobile App's native Mode Handling
//def getModeOk() {
//	def result = !modes || modes.contains(location.mode)
//	log.debug "modeOk = $result"
//	return result
//}

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
	log.debug "daysOk = $result"
	return result
}
