/**
 *  Omer Counting
 *
 *  Author: iceberg
 *  Date: 2018-04-05
 *  v0.3 - added voice annoucements
 *  v0.2 - added push notifications
 *  v0.1 - initial release
 */

// Automatically generated.
definition(
    name: "Omer",
    namespace: "aniceberg",
    author: "iceberg",
    description: "Announces the Omer each day after nightfall",
    category: "My Apps",
    iconUrl: "http://upload.wikimedia.org/wikipedia/commons/thumb/4/49/Star_of_David.svg/200px-Star_of_David.svg.png",
    iconX2Url: "http://upload.wikimedia.org/wikipedia/commons/thumb/4/49/Star_of_David.svg/200px-Star_of_David.svg.png",
    iconX3Url: "http://upload.wikimedia.org/wikipedia/commons/thumb/4/49/Star_of_David.svg/200px-Star_of_David.svg.png")

preferences {
	page(name: "mainPage", title: "Setup your Omer reminders", install: true, uninstall: true)	
}

def mainPage() {
	dynamicPage(name: "mainPage") {
	
    section("Location") {
    	input "autoLocation", "bool", title: "Enable automatic detection:", defaultValue: true, submitOnChange: true
        
	if (autoLocation=="false") {
        	input"locationZIP", "number", title: "Enter zipcode:", required: true, defaultValue: "11223", range: "0..99999"
        }
    }
	section("Remind me each day at:") {
		input "offset", "number", title: "How many minutes after sunset? (Typically 40 or 50) Set to '0' to set a manual reminder time.", defaultValue: "40", range: "0..*",  required: true, submitOnChange: true
	    if (offset==0) {
    		input "theTime", "time", title: "Select manual reminder time (e.g. '9:00PM'):"
		}
	}
	section("Notifications") {
		input("recipients", "contact", required: false, title: "Send notifications to:") {
			input "sendPushMessage", "bool", title: "Enable push notifications:", required: false, defaultValue: false
            input "phone", "phone", title: "Send text message to:", description: "Phone number", required: false
			}
		}
	section(hideWhenEmpty: true, "Announce Omer reminder on smart speakers?") {
		input "speakers", "capability.speechSynthesis", title: "On these speakers", multiple:true, required: false
        input "speakerVolume", "number", title: "Select reminder volume", description: "0-100%", required: false
		}
	//section("More options"){
	//	label(name: "label", required: false, multiple: false)
	//input "modes", "mode", title: "Only remind me when mode is", multiple: true, required: false
    //mode(name: "modeMultiple", title: "Only remind me when mode is", multiple: true, required: false)
	//}
	}
}
		
		
def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    unsubscribe()
	initialize()
}

def initialize() {
    
	if (offset>0){
    	if (autoLocation=="true") {
        subscribe(location, "sunsetTime", sunsetTimeHandler)
    	scheduleRemind(location.currentValue("sunsetTime"))
        } else {
			def s = getSunriseAndSunset(zipCode: locationZIP, sunsetOffset: +offset)
        	log.debug "Manual schedule for ${locationZIP} local sunset is ${s.sunset}"
        	schedule(s.sunset, poll)
        }
	} else if (offset==0) {
    	log.debug "Reminder time set to ${theTime} for all days."
        schedule(theTime, poll)
	}
}

def sunsetTimeHandler(event) {
    //when I find out the sunset time, schedule the Omer reminder with an offset
    log.debug "Sunset for location is ${event.value}"
    scheduleRemind(event.value)
}

def scheduleRemind(sunsetString) {
    //get the Date value for the string
    def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunsetString)
    log.debug "Sunset is ${sunsetTime}"

    //calculate the offset
    def timeAfterSunset = new Date(sunsetTime.time + (offset * 60 * 1000))
    log.debug "Automatically scheduled for: ${timeAfterSunset} (which is ${offset} minutes after sunset at ${sunsetTime})"

    //schedule this to run
    schedule(timeAfterSunset, poll)
}

//Check hebcal for today's Omer count
def poll() {
	//insert Mode() test here
    Hebcal_WebRequest()
}//END def poll()


/**********************************************
// HEBCAL FUNCTIONS
-----------------------------------------------*/

//This function is the web request and response parse
def Hebcal_WebRequest(){
	def today = new Date().format("yyyy-MM-dd")
	//def today = "2018-04-04"
	//def time = new Time().format
	def hebcal_date
	def hebcal_category
	def hebcal_title
	def hebcal_hebrew
	def pushMessage
    def speakMessage
    def urlRequestOmer = "http://www.hebcal.com/hebcal/?v=1&cfg=json&c=off&year=now&o=on&lg=sh"
	log.trace "${urlRequestOmer}"

	def hebcal = { response ->
	hebcal_date = response.data.items.date
	hebcal_category = response.data.items.category
	hebcal_title = response.data.items.title
	hebcal_hebrew = response.data.items.hebrew
    
	for (int i = 0; i < hebcal_date.size; i++) {
		if(hebcal_date[i]==today){
			if(hebcal_category[i]=="omer"){
                pushMessage = "Tonight is ${hebcal_hebrew[i]}, the ${hebcal_title[i]}"
				speakMessage = "Tonight is the ${hebcal_title[i].split("day")[0]} night of the Ohhmer"
				handleMessages(pushMessage,speakMessage)
			}//END if(hebcal_category[i]=="omer")
		}//END if(hebcal_date[i]==today)
	}//END for (int i = 0; i < hebcal_date.size; i++)
 }//END def hebcal = { response ->

httpGet(urlRequestOmer, hebcal);
}//END def queryHebcal()


def handleMessages(textmsg,speakmsg){
	if (location.contactBookEnabled && recipients !="") {
		sendNotificationToContacts(textmsg, recipients)
		log.debug "Contact Book enabled!"
		log.debug "Message sent!"
	} else if (phone) { // check that the user did select a phone number
		sendSms( phone, textmsg )
		log.debug "Contact Book not enabled"
		log.debug "sending text message"
        } 
	if (sendPushMessage) { // check that the user selected push notifications
        	sendPush( textmsg )
	}//END IF (sendPush)
	if (speakers) { //check if speakers are available and selected
    	if (speakerVolume) {
        	speakers?.setLevel(speakerVolume)
            }
    	log.debug "Speakers: ${speakers} enabled."
        speakers?.speak(speakmsg)
	}//END IF (speakers)	
}//END def handleMessages(textmsg,speakmsg)

