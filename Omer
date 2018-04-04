/**
 *  Omer Counting
 *
 *  Author: iceberg
 *  Date: 2018-04-04
 */

// Automatically generated.
definition(
    name: "ספירת העומר",
    namespace: "ShabbatHolidayMode",
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
	section( "Notifications" ) {
        	input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
        	input "phone", "phone", title: "Send a Text Message?", required: false
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
log.trace "${urlRequestOme}"

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
if ( sendPushMessage != "No" ) {
        log.debug( "sending push message" )
        //sendPush( msg )
    }

    if ( phone ) {
        log.debug( "sending text message" )
        sendSms( phone, msg )
    }
}//END def sendMessage(msg)
