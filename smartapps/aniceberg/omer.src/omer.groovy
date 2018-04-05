/**
 *  Omer Counting
 *
 *  Author: iceberg
 *  Date: 2018-04-05
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
	section("Run each day at:") {
		input "theTime", "time", title: "Time to execute every day"
		}
	section("Send Notifications?") {
        	input("recipients", "contact", title: "Send notifications to") {
            		input "phone", "phone", title: "Send via text message",
			description: "Phone Number", required: false
      		 	}
    		}
	section("Enable Push Notifications?") {
				input "sendPush", "bool", required: false, title: "Enable push notifications"
		}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	initialize()
}

def initialize() {
    poll();
    schedule(theTime, poll) 
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
def urlRequestOmer = "http://www.hebcal.com/hebcal/?v=1&cfg=json&c=off&year=now&o=on&lg=sh"
log.trace "${urlRequestOmer}"

def hebcal = { response ->
	hebcal_date = response.data.items.date
	hebcal_category = response.data.items.category
	hebcal_title = response.data.items.title
	hebcal_hebrew = response.data.items.hebrew
    
    for (int i = 0; i < hebcal_date.size; i++) 
    {
	if(hebcal_date[i]==today){
        	if(hebcal_category[i]=="omer"){
	        	pushMessage = "Tonight is ${hebcal_hebrew[i]}, the ${hebcal_title[i]}"
				sendMessage(pushMessage)
    			log.debug pushMessage
          	}//END if(hebcal_category[i]=="omer")
	}//END if(hebcal_date[i]==today)
    }//END for (int i = 0; i < hebcal_date.size; i++)
 }//END def hebcal = { response ->
httpGet(urlRequestOmer, hebcal);
}//END def queryHebcal()


def sendMessage(msg){
	if (location.contactBookEnabled && recipients) {
		sendNotificationToContacts(msg, recipients)
		log.debug "Contact Book enabled!"
		log.debug "Sending push message"
	} else if (phone) { // check that the user did select a phone number
		sendSms( phone, msg )
		log.debug "Contact Book not enabled"
		log.debug "sending text message"
        } else if (sendPush) { // check that the user selected push notifications
        	sendPush( phone, msg )
	}
}//END def sendMessage(msg)
