//Release History
//		1.0 May 20, 2016
//			Initial Release


metadata {
        definition (name: "Housesitter Presence", namespace: "HomeLit", author: "iceberg") {
        capability "Switch"
        capability "Refresh"
        capability "Presence Sensor"
		capability "Sensor"
        
		command "sitting"
		command "notsitting"
    }

	// simulator metadata
	simulator {
	}

	// UI tile definitions
	tiles {
        standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
			state("present", action: "sitting", labelIcon:"st.presence.tile.mobile-present", backgroundColor:"#53a7c0", nextState: "not present")
			state("not present", action: "notsitting", labelIcon:"st.presence.tile.mobile-not-present", backgroundColor:"#ffffff", nextState: "present")
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        standardTile("button", "device.switch", width: 1, height: 1) {
			state "off", label: 'OFF', action: "switch.on", icon: "st.Kids.kid10", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'ON', action: "switch.off", icon: "st.Kids.kid10", backgroundColor: "#53a7c0", nextState: "off"
		}

		main (["presence", "button"])
		details(["presence", "button", "refresh"])
	}
}

def parse(String description) {
	def pair = description.split(":")
	createEvent(name: pair[0].trim(), value: pair[1].trim())
}

// handle commands
def sitting() {
	on()
}


def notsitting() {
    off()
}

def on() {
	sendEvent(name: "switch", value: "on")
    sendEvent(name: "presence", value: "present")

}

def off() {
	sendEvent(name: "switch", value: "off")
    sendEvent(name: "presence", value: "not present")

}