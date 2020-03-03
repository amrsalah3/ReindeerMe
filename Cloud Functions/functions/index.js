const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

module.exports = {
	...require("./friends.js"),
	...require("./chats.js"),
	...require("./msgStatus.js"),
	...require("./changeAvatar.js")
};
