definition(
    name: "Tamperproof Lighting",
    namespace: "homeLit/automations",
    author: "iceberg",
    description: "A simple app to create tamperproof lighting schedules. This is a child app.",
    category: "My Apps",

    // the parent option allows you to specify the parent app in the form <namespace>/<app name>
    parent: "homeLit/parent:Tamperproof Lighting",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page name: "mainPage", title: "Automate Lights & Switches", install: false, uninstall: true, nextPage: "namePage"
    page name: "namePage", title: "Automate Lights & Switches", install: true, uninstall: true
    page name: "timeSetPage", title: "How to trigger the switch"
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unschedule()
    initialize()
}

def initialize() {
    // if the user did not override the label, set the label to the default
    if (!overrideLabel) {
        app.updateLabel(defaultLabel())
    }
    // schedule the turn on and turn off handlers
    schedule(scheduleStartTime, startHandler)
    schedule(scheduleEndTime, endHandler)
    schedule(lights, "lights.on", lightOn)
	schedule(lights, "lights.off", lightOff)
}

// main page to select lights, the action, and turn on/off times
def mainPage() {
    dynamicPage(name: "mainPage") {
        section {
            lightInputs()
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
            input "overrideLabel", "bool", title: "Edit automation name", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}

// inputs to select the lights
def lightInputs() {
    input "lights", "capability.switch", title: "Which switches do you want to control?", multiple: true, submitOnChange: true
}

// inputs to control what to do with the lights (turn on/keep on, turn off/keep off)
def actionInputs() {
    if (lights) {
    	input "action", "enum", title: "What do you want to do?", options: ["off":"Turn off", "on":"Turn on"], required: true, submitOnChange: true
    }
}

// inputs for selecting on and off time
def timeInputs() {
    if (settings.action) {
    	section {
	    	href "timeSetPage", title: "When do you want to schedule the switches?"
        }      
	}
    def scheduleStartTime = "toggleStartTime"
    def scheduleEndTime = "toggleEndTime"
    
}

// page for selecting automation triggers
def timeSetPage() {
	dynamicPage(name: "timeSetPage") {
    	section ("By time") {
            input "timeStartOptions", "enum", title: "When to schedule switch ${action}?", multiple: false, options: ["sunrise":"At sunrise", "sunset":"At sunset", "manualTime":"At a specified time"], submitOnChange: true
                if (timeStartOptions == "manualTime") {
                    input "toggleStartTime", "time", title: "Time to toggle switch ${action}", required: true
                } else {
                	section("Location") {
    					input "autoLocation", "bool", title: "Enable automatic detection:", defaultValue: true, submitOnChange: true
        				if (autoLocation=="false") {
				        	input"locationZIP", "number", title: "Enter zipcode:", required: true, defaultValue: "11223", range: "0..99999"
       					}
    				}
                	input "sOffsetStartTime", "number", title: "+/- minutes from ${timeStartOptions}. (e.g. Enter '-30' for half hour before ${timeStartOptions})", defaultValue: "0", range: "-719..719", required = false
                }
			input "timeEndOptions", "enum", title: "When to end ${action} schedule?", multiple: false, options: ["sunrise":"At sunrise", "sunset":"At sunset", "manualTime":"At a specified time"], submitOnChange: true
            	if (timeEndOptions == "manualTime") {
                    input "toggleEndTime", "time", title: "Time to toggle switch ${action}", required: true
                } else {
                	input "sOffsetEndTime", "number", title: "Optional: Choose +/- minutes from ${timeEndOptions}. (e.g. Enter '-30' for half hour before ${timeEndOptions})", defaultValue: "0", range: "-719..719", required: true
                }
			}
	
		section ("By specific mode") {
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false, submitOnChange: true
    	}
	}
}

def tamperProtection() {
	section ("Tamper Protection") {
    	input "tamperProtection", "bool", title: "Prevent switches from being toggled during schedule?",  defaultValue: "false", required: "false", submitOnChange: true
        if (tamperProtection) {
           	input "desiredState", "enum", title: "Keep switches turned...", multiple: false, options: ["on":"on", "off":"off"], required: true, submitOnChange: true
           	if (lights.find{it.hasCommand('tapUp2')} != null) {
        		switchOverrides()
           	}
   		}
	}
}

def switchOverrides() {
	input "switchOverrideCmd", "enum", title: "Override tamper protection with double, triple or long hold?", 
	options: ["false":"No", "tapUp2":"Double-Tap Up", "tapDown2":"Double-Tap Down", "tapUp3":"Triple-Tap Up", "tapDown3":"Triple-Tap Down", "holdUp":"Hold Up", "holdDown":"Hold Down"],
    multiple: false, defaultValue: "No", required: false
}

def motionOverrides() {
	section ("Motion Detection Rules") {
        input "motion", "capability.motionSensor", title: "Which motion sensors should be checked for activity?", multiple: true, required: "false"
    }
}

// the handler method that turns and keeps the lights ON 
def startHandler() {
    // switch on the selected action
    switch(action) {
        case "on":
            log.debug "on()"
            lights.on()
            break
		case "off":
            log.debug "off()"
            lights.off()
            break
        }
}

// the handler method that turns and keeps the lights OFF 
def endHandler() {
	switch(action) {
        case "on":
            log.debug "End on() Schedule"
            lights.on()
            break
		case "off":
            log.debug "End off() Schedule"
            lights.off()
            break
	}
}

def lightOn(evt) {
  log.trace "lightOn($evt.name: $evt.value)"
  def delay = (openThreshold != null && openThreshold != "") ? openThreshold * 60 : 600
  runIn(delay, doorOpenTooLong, [overwrite: true])
}

def lightOff(evt) {
  log.trace "lightOff($evt.name: $evt.value)"
  unschedule(doorOpenTooLong)
}





// a method that will set the default label of the automation.
// It uses the lights selected and action to create the automation label
def defaultLabel() {
    def lightsLabel = settings.lights.size() == 1 ? lights[0].displayName : lights[0].displayName + ", etc..."
        "Turn $action $lightsLabel"
}

// utility method to get a map of available actions for the selected switches
def actionMap() {
    def map = [on: "Turn On", off: "Turn Off"]
    if (lights.find{it.hasCommand('setLevel')} != null) {
        map.level = "Turn On & Set Level"
    }
    if (lights.find{it.hasCommand('setColor')} != null) {
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

    lights.each {
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