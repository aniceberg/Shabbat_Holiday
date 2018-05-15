definition(
    name: "Tamperproof Lighting",
    namespace: "HomeLit/automations",
    author: "iceberg",
    description: "A simple app to create tamperproof lighting schedules. This is a child app.",
    category: "My Apps",

    // the parent option allows you to specify the parent app in the form <namespace>/<app name>
    parent: "HomeLit/parent:Tamperproof Lighting",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page name: "mainPage", title: "Automate Lights & Switches", install: false, uninstall: true, nextPage: "namePage"
    page name: "namePage", title: "Automate Lights & Switches", install: true, uninstall: true
    page name: "timeSetPage", title: "When to begin schedule"
    page name: "timeEndPage", title: "When to end scheduling"
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unschedule()
    unsubscribe()
    initialize()
}

def initialize() {
    // if the user did not override the label, set the label to the default
    if (!overrideLabel) {
        app.updateLabel(defaultLabel())
    }
  
    // schedule the turn on and turn off handlers
    schedule(toggleStartTime, startHandler)
    schedule(toggleEndTime, endHandler)
    subscribe(switches, "switches.on", switchesOn)
	subscribe(switches, "switches.off", switchesOff)
}

// main page to select lights, the action, and turn on/off times
def mainPage() {
    dynamicPage(name: "mainPage") {
        section {
            switchInputs()
            actionInputs()
        	timeInputs()
            tamperProtection()
            switchOverrides()
            motionOverrides()
        }
        
    }
}

// page for allowing the user to give the automation a custom name
def namePage() {
    if (!overrideLabel) {
        // if the user selects to not change the label, give a default label
        def l = defaultLabel()
        log.debug "will set default label of $l"
        app.updateLabel(l)
    }
    dynamicPage(name: "namePage") {
        if (overrideLabel) {
            section("Automation name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } else {
            section("Automation name") {
                paragraph app.label
            }
        }
        section {
            input "overrideLabel", "bool", title: "Edit automation name", defaultValue: false, required: false, submitOnChange: true
        }
    }
}

// inputs to select the lights switches
def switchInputs() {
    input "switches", "capability.switch", title: "Which switch do you want to control?", multiple: false, submitOnChange: true
}

// inputs to control what to do with the lights (turn on/keep on, turn off/keep off)
def actionInputs() {
    if (switches) {
    	input "desiredAction", "enum", title: "What do you want to do?", options: ["off":"Turn off", "on":"Turn on"], required: true, submitOnChange: true
    }
}

// inputs for selecting on and off time
def timeInputs() {
    if (settings.action) {
    	section ("Select time or mode for scheduling") {
        	input "startOptions", "enum", title: "When to schedule switch ${desiredAction}?", multiple: false, options: ["sunrise":"At sunrise", "sunset":"At sunset", "manualTime":"At a specified time", "modeBased":"When mode changes"], submitOnChange: true
	    	if (startOptions != "modeBased") {
            	href "timeSetPage"
        	} else if (settings.modes) {
            	input "modes", "mode", title: "Only when mode is", multiple: true, required: false
            }
        }
        section ("Select time or mode to end scheduling") {
			input "endOptions", "enum", title: "When to end ${desiredAction} schedule?", multiple: false, options: ["sunrise":"At sunrise", "sunset":"At sunset", "manualTime":"At a specified time", "modeBased":"When mode changes"], submitOnChange: true
			if (startOptions != "modeBased") {
            	href "timeEndPage"
			} else if (settings.modes) {
            	input "modes", "mode", title: "Only when mode is", multiple: true, required: false
            }
		}
	}
}

// page for selecting automation triggers
def timeSetPage() {
	dynamicPage(name: "timeSetPage") {
    	section ("Select time to begin scheduling") {
 			if (startOptions == "manualTime") {
            	input "toggleStartTime", "time", title: "Time to toggle switch ${desiredAction}", required: true
            } else { 
				input "sOffsetStartTime", "number", title: "Optional: +/- minutes from ${startOptions.value}. (e.g. Enter '-30' for half hour before ${startOptions.value})", defaultValue: "0", range: "-719..719", required = false
				//subscribe(location, "${startOptions.value}")
                //def toggleStartTime = getSunriseAndSunset([startOptions.value]Offset: +sOffsetStartTime)
                }  
		}
    }
}

def timeEndPage() {
	dynamicPage(name: "timeEndPage") {        
        section ("Select time to end scheduling") {			
            if (endOptions == "manualTime") {
            	input "toggleEndTime", "time", title: "Time to toggle switch ${desiredAction}", required: true
            } else {
    			input "sOffsetEndTime", "number", title: "Optional: Choose +/- minutes from ${endOptions.value}. (e.g. Enter '-30' for half hour before ${endOptions.value})", defaultValue: "0", range: "-719..719", required: true
            	//def toggleEndTime = getSunriseAndSunset(location, {endOptions.value}Offset: +sOffsetEndTime)
            }
		}
		//def endS = getSunriseAndSunset(zipCode: locationZIP, sunriseOffset: +sOffsetStartTime, sunsetOffset: +sOffsetStartTime)
    	//log.debug "Manual schedule for ${locationZIP} local sunrise is ${s.sunrise}, sunset is ${s.sunset}."
	}
}


def tamperProtection() {
	section ("Tamper Protection") {
    	input "tamperProtectionOption", "bool", title: "Prevent switches from being toggled during schedule?",  defaultValue: false, required: false, submitOnChange: true
        if (tamperProtectionOption) {
           	input "desiredState", "enum", title: "Keep switches turned...", multiple: false, options: ["on":"on", "off":"off"], required: true, submitOnChange: true
           	input "delayTime", "number", title: "Optional: Delay time in minutes before turning switch back ${desiredState}. (e.g. 1 or 5 minutes; default is 0 minutes.)",
            	range: "0..719", defaultValue: "0", required: false
			delayTime = delayTime * 1000 * 60
            //if (switches.find{it.hasCommand('tapUp2')} != null) {
        	//	def switchOverrideEnabled = null
            //    switchOverrides()
            //    if (switchOverrideCmd.value != false) {
            //    	switchOverrideEnabled = true
            //    }
           	//}
   		}
	}
}

def switchOverrides() {
	input "switchOverrideCmd", "enum", title: "Override tamper protection with double/triple-tap or long hold?", 
		options: ["false":"No", "tapUp2":"Double-Tap Up", "tapDown2":"Double-Tap Down", "tapUp3":"Triple-Tap Up", "tapDown3":"Triple-Tap Down", "holdUp":"Hold Up", "holdDown":"Hold Down"],
		multiple: false, defaultValue: "No", required: false
}

def motionOverrides() {
	section ("Motion Detection Rules") {
        input "motion", "capability.motionSensor", title: "Which motion sensors should be checked for activity?", multiple: true, required: false
    }
}

// the handler method that turns and keeps the lights either ON or OFF 
def startHandler() {
    // switch on the selected action
    switch(desiredAction) {
        case "on":
            log.debug "Starting $switches ON schedule and turning switches ON"
            switches.on()
            break;
		case "off":
            log.debug "Starting $switches OFF schedule and turning switches OFF"
            switches.off()
            break;
        }
	def scheduledRun = true
}

// the handler method when the lighting schedule is finished
def endHandler() {
	switch(desiredAction) {
        case "on":
            log.debug "Ending $switches ON schedule and turning switches OFF"
            switches.off()
            break;
		case "off":
            log.debug "Ending $switches OFF schedule and leaving switches OFF"
            //switches.off()
            break;
	}
    scheduledRun = null
}

def switchesOn(evt) {
	log.trace "switchesOn($evt.name: $evt.value)"
	if (scheduledRun && desiredState == "off") {
		//Checks if either no override allowed or if override command matches the event receieved
		log.debug "Overide is '$switchOverrideEnabled'. Allowed override button is '$switchOverrideCmd'."
		//if (!switchOverrideEnabled || switchOverrideCmd == evt.value) { 
    	runIn(delayTime, switches.off())
    //}
  }
}

def switchesOff(evt) {
	log.trace "switchesOff($evt.name: $evt.value)"
	if (scheduledRun && desiredState == "on") {
		//Checks if either no override allowed or if override command matches the event receieved
        log.debug "Overide is '$switchOverrideEnabled'. Allowed override button is '$switchOverrideCmd'."
  		//if (!switchOverrideEnabled || switchOverrideEnabled == evt.value) {
    	runIn(delayTime, switches.on())
    //}
  }
}





// a method that will set the default label of the automation.
// It uses the lights selected and action to create the automation label
def defaultLabel() {
    def switchesLabel = settings.switches.size() == 1 ? switches[0].displayName : switches[0].displayName + ", etc..."
        "Turn $desiredAction $switchesLabel"
}

// utility method to get a map of available actions for the selected switches
def actionMap() {
    def map = [on: "Turn On", off: "Turn Off"]
    if (switches.find{it.hasCommand('setLevel')} != null) {
        map.level = "Turn On & Set Level"
    }
    if (switches.find{it.hasCommand('setColor')} != null) {
        map.color = "Turn On & Set Color"
    }
    map
}

// utility method to collect the action map entries into maps for the input
def actionOptions() {
    actionMap().collect{[(it.key): it.value]}
}

// set the color and level as specified, if the user selected to set color.
def setColor() {

    def hueColor = 0
    def saturation = 100

    switch(color) {
            case "White":
            hueColor = 52
            saturation = 19
            break;
        case "Daylight":
            hueColor = 53
            saturation = 91
            break;
        case "Soft White":
            hueColor = 23
            saturation = 56
            break;
        case "Warm White":
            hueColor = 20
            saturation = 80
            break;
        case "Blue":
            hueColor = 70
            break;
        case "Green":
            hueColor = 39
            break;
        case "Yellow":
            hueColor = 25
            break;
        case "Orange":
            hueColor = 10
            break;
        case "Purple":
            hueColor = 75
            break;
        case "Pink":
            hueColor = 83
            break;
        case "Red":
            hueColor = 100
            break;
    }

    def value = [switch: "on", hue: hueColor, saturation: saturation, level: level as Integer ?: 100]
    log.debug "color = $value"

    switches.each {
        if (it.hasCommand('setColor')) {
            log.debug "$it.displayName, setColor($value)"
            it.setColor(value)
        } else if (it.hasCommand('setLevel')) {
            log.debug "$it.displayName, setLevel($value)"
            it.setLevel(level as Integer ?: 100)
        } else {
            log.debug "$it.displayName, on()"
            it.on()
        }
    }
}