const functions = require('firebase-functions');
const admin = require('firebase-admin');

exports.handleMessage = functions.database
  .ref('users/{user}/chats/{receiver}/{msgkey}')
  .onCreate(
  (snapshot, context) => {
	const senderId = snapshot.child("sender_id").val();
	if (context.params.user != senderId){return null;} // If cloud functions add the message to the receiver's node, don't trigger again.
	const senderName = snapshot.child("sender_name").val();
	const msgContent = snapshot.child("msg_content").val();
	const msgType = snapshot.child("msg_type").val();
	const receiverId = context.params.receiver;
	const msgKey = context.params.msgkey;
	const replyKey = snapshot.child("reply_key").val();

	var msgBrief = msgContent;
	if (msgContent.length > 50){
		msgBrief = msgContent.substring(0,50) + "...";
	}
	
	// Send the message to the receiver's chat node
	const messageObj = {
		sender_name: senderName,
		sender_id: senderId,
		msg_content: msgContent,
		msg_type: msgType,
		reply_key: replyKey,
		msg_status: 1, // sent (but not delivired)
		deleted: 0,
		date_in_millis: Date.now(),
		date: (new Date()).toString()
	};
	console.log('MESSAGEOBJ', messageObj);
	snapshot.ref.parent.parent.parent.parent.child(receiverId).child('chats').child(senderId).child(msgKey).set(messageObj).then(()=>{
		snapshot.ref.parent.parent.parent.parent.child(receiverId).child('friends').child(senderId).child("last_msg").set(messageObj).then(()=>{
			
			// Get receiver token from database then send data message notification to the user
			admin.database().ref(`/users/${receiverId}/registration_token`).once("value", function(tokenSnapshot) {
				admin.database().ref(`/users/${senderId}/ppurl`).once("value", (senderAvatarSnap) => {
					const ppurl = senderAvatarSnap.val();
					var regToken = tokenSnapshot.val();
					var message = {
						token : regToken,
						data : {
							fn_call_code: "chat",
							sender_name: senderName,
							sender_id: senderId,
							sender_pp: ppurl,
							msg_content: msgContent,
							msg_type: msgType+"", // to String cuz firebase FCM does not support numbers
							msg_status: "1", // sent (but not delivered)
							msg_key: msgKey,
							receiver_id: receiverId
						}
						
					};  
					console.log('MESSAGE', message);
					// Now sending notification to the receiver
					return admin.messaging().send(message)
						.then((response) => {
							// Response is a message ID string.
							console.log('Successfully sent notification.', response);
							})
						.catch((error) => {
							console.log('Error sending notification: ', error);
							 });
					});
				 
		  
			}, function (errorObject) {
			  console.log("Token read failed: " + errorObject.code);
			});
		});
	});

}
);