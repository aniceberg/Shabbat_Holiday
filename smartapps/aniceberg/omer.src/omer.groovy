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
	page(name: "ttsSettings", title: "Text to Speech settings")
}

def mainPage() {
	dynamicPage(name: "mainPage") {
	
    section("Location:") {
    	input "autoLocation", "boolean", title: "Enable automatic detection:", defaultValue: true, submitOnChange: true
        
	if (!autoLocation) {
        	input"locationZIP", "number", title: "Enter zipcode:", required: true, defaultValue: "11223", range: "0..99999"
        }
    }
	section("Remind me each day at:") {
		input "offset", "number", title: "How many minutes after sunset? (Typically 40 or 50) Set to '0' to manually choose a time.", defaultValue: "40", range: "0..*",  required: true, submitOnChange: true
	}
    if (offset==0) {
    	section {
    		input "theTime", "time", title: "Remind at"
            }
	}	
	section("Send Notifications?") {
		input("recipients", "contact", required: false, title: "Send notifications to") {
			input "phone", "phone", title: "Send via text message", description: "Phone Number", required: false
			}
		}
	section("Enable Push Notifications?") {
		input "sendPushMessage", "bool", required: false, title: "Enable push notifications"
		}
	section(hideWhenEmpty: true, "Announce Omer reminder on smart speakers?") {
		input "speakers", "capability.musicPlayer", title: "On these speakers", multiple:true, required: false
        input "volume", "number", title: "Select reminder volume", description: "0-100%", required: false
		input "onPlay", "bool", title: "Only announce if nothing playing?", required: false, defaultValue: false
		input "resumePlaying", "bool", title: "Resume currently playing audio after notification?", required: false, defaultValue: true
        href "ttsSettings", title: "Text for Speech Settings",required:false, description:ttsMode
		}
	section("More options", hideable: true, hidden: true) {
		input "days", "enum", title: "Only announce on certain days of the week?", multiple: true, required: false,
				options: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
		}
	}
}
def ttsSettings() {
	dynamicPage(name: "ttsSettings") {
        def languageOptions = ["ca-es":"Catalan","zh-cn":"Chinese (China)","zh-hk":"Chinese (Hong Kong)","zh-tw":"Chinese (Taiwan)","da-dk":"Danish","nl-nl":"Dutch","en-au":"English (Australia)","en-ca":"English (Canada)","en-gb":"English (Great Britain)","en-in":"English (India)","en-us":"English (United States)","fi-fi":"Finnish","fr-ca":"French (Canada)","fr-fr":"French (France)","de-de":"German","it-it":"Italian","ja-jp":"Japanese","ko-kr":"Korean","nb-no":"Norwegian","pl-pl":"Polish","pt-br":"Portuguese (Brazil)","pt-pt":"Portuguese (Portugal)","ru-ru":"Russian","es-mx":"Spanish (Mexico)","es-es":"Spanish (Spain)","sv-se":"Swedish (Sweden)"]
 		section() {
            input "ttsMode", "enum", title: "Mode?", required: true, defaultValue: "SmartThings",submitOnChange:true, options: ["SmartThings","Google","Alexa"]
            input "stLanguage", "enum", title: "SmartThings Voice?", required: true, defaultValue: "en-US Salli", options: ["da-DK Naja","da-DK Mads","de-DE Marlene","de-DE Hans","en-US Salli","en-US Joey","en-AU Nicole","en-AU Russell","en-GB Amy","en-GB Brian","en-GB Emma","en-GB Gwyneth","en-GB Geraint","en-IN Raveena","en-US Chipmunk","en-US Eric","en-US Ivy","en-US Jennifer","en-US Justin","en-US Kendra","en-US Kimberly","es-ES Conchita","es-ES Enrique","es-US Penelope","es-US Miguel","fr-CA Chantal","fr-FR Celine","fr-FR Mathieu","is-IS Dora","is-IS Karl","it-IT Carla","it-IT Giorgio","nb-NO Liv","nl-NL Lotte","nl-NL Ruben","pl-PL Agnieszka","pl-PL Jacek","pl-PL Ewa","pl-PL Jan","pl-PL Maja","pt-BR Vitoria","pt-BR Ricardo","pt-PT Cristiano","pt-PT Ines","ro-RO Carmen","ru-RU Tatyana","ru-RU Maxim","sv-SE Astrid","tr-TR Filiz"]
            input "googleLanguage", "enum", title: "Google Voice?", required: true, defaultValue: "en", options: ["af":"Afrikaans","sq":"Albanian","ar":"Arabic","hy":"Armenian","ca":"Catalan","zh-CN":"Mandarin (simplified)","zh-TW":"Mandarin (traditional)","hr":"Croatian","cs":"Czech","da":"Danish","nl":"Dutch","en":"English","eo":"Esperanto","fi":"Finnish","fr":"French","de":"German","el":"Greek","ht":"Haitian Creole","hi":"Hindi","hu":"Hungarian","is":"Icelandic","id":"Indonesian","it":"Italian","ja":"Japanese","ko":"Korean","la":"Latin","lv":"Latvian","mk":"Macedonian","no":"Norwegian","pl":"Polish","pt":"Portuguese","ro":"Romanian","ru":"Russian","sr":"Serbian","sk":"Slovak","es":"Spanish","sw":"Swahili","sv":"Swedish","ta":"Tamil","th":"Thai","tr":"Turkish","vi":"Vietnamese","cy":"Welsh"]
            input "ttsLanguage", "enum", title: "TTS Language?", required: true, defaultValue: "en-us",options: languageOptions
            input "ttsApiKey", "text", title: "Alexa Access Key", required: ttsMode == "Alexa" ? true:false,  defaultValue:"millave"
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
	initialize()
}

def initialize() {
    
	if (offset!=0){
    	if (autoLocation) {
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
def poll()
{
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
                speakMessage = "Tonight is the ${hebcal_title[i]}"
				sendMessage(pushMessage,speakMessage)
				log.debug pushMessage
                log.debug speakMessage
			}//END if(hebcal_category[i]=="omer")
		}//END if(hebcal_date[i]==today)
	}//END for (int i = 0; i < hebcal_date.size; i++)
 }//END def hebcal = { response ->

httpGet(urlRequestOmer, hebcal);
}//END def queryHebcal()


def sendMessage(textmsg,speakmsg){
	if (location.contactBookEnabled && recipients) {
		sendNotificationToContacts(textmsg, recipients)
		log.debug "Contact Book enabled!"
		log.debug "Sending push message"
	} else if (phone) { // check that the user did select a phone number
		sendSms( phone, textmsg )
		log.debug "Contact Book not enabled"
		log.debug "sending text message"
        } 
	if (sendPushMessage) { // check that the user selected push notifications
        	sendPush( textmsg )
	}//END IF (sendPush)
	if (speakers) { //check if speakers are selected
    	log.debug "Speakers: $speakers enabled."
		safeTextToSpeech(speakmsg)
		if(ttsMode == "Alexa" && !message.contains("#s")) {
                	speaker.playTrack(speakmsg.uri)
            }else {
            	speaker.playTrackAndResume(speakmsg.uri, speakmsg.duration, volume)
            }
	}//END IF (speakers)	
}//END def sendMessage(msg)

private safeTextToSpeech(message) {
	log.debug "Encoding: '$message' to speech"
    switch(ttsMode){
		case "Alexa":
		log.debug "Alexa TTS Mode called"
		[uri: "x-rincon-mp3radio://tts.freeoda.com/alexa.php/" + "?key=$alexaApiKey&text=" + URLEncoder.encode(message, "UTF-8").replaceAll(/\+/,'%20') +"&sf=//s3.amazonaws.com/smartapp-" , duration: "${5 + Math.max(Math.round(message.length()/12),2)}"]
		break
		default:
		try {
            log.debug "Attempting default TTS method"
            textToSpeech(message,stLanguage.substring(6))
		}
		catch (Throwable t) {
			log.error "Error in attempting default TTS method"
			textToSpeechT(message)
		}
		break
    }
}

private textToSpeechT(message){
    if (message) {
    	if (ttsApiKey){
            [uri: "x-rincon-mp3radio://api.voicerss.org/" + "?key=$ttsApiKey&hl=en-us&r=0&f=48khz_16bit_mono&src=" + URLEncoder.encode(message, "UTF-8").replaceAll(/\+/,'%20') +"&sf=//s3.amazonaws.com/smartapp-" , duration: "${5 + Math.max(Math.round(message.length()/12),2)}"]
        }else{
        	message = message.length() >100 ? message[0..90] :message
        	[uri: "x-rincon-mp3radio://www.translate.google.com/translate_tts?tl=en&client=t&q=" + URLEncoder.encode(message, "UTF-8").replaceAll(/\+/,'%20') +"&sf=//s3.amazonaws.com/smartapp-", duration: "${5 + Math.max(Math.round(message.length()/12),2)}"]
     	}
    }
}
