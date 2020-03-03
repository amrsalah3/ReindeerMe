const functions = require('firebase-functions');
const admin = require('firebase-admin');

exports.handleMsgStatus = functions.database
.ref('users/{deliveredToWhom}/chats/{friend}/{msgKey}/msg_status')
.onUpdate((change, context) => {
	
	const deliveredToWhom = context.params.deliveredToWhom;
	const friend = context.params.friend;
	const msgKey = context.params.msgKey;
	const newStatus = change.after.val();
	// Check if this trigger is not triggered by cloud functions when dealing with a last trigger
	admin.database().ref(`/users/${deliveredToWhom}/chats/${friend}/${msgKey}/sender_id`).once("value", function(senderIdSnap) {
		if (deliveredToWhom == senderIdSnap.val()){return null;} // If yes stop the function
		// If new status is late status (due to server latency) and there has been already an older one with higher status then don't change it
		if (newStatus > change.before.val() && senderIdSnap.val() != null){
			return change.after.ref.parent.parent.parent.parent.parent.child(`${friend}/chats/${deliveredToWhom}/${msgKey}/msg_status`).set(newStatus)
			.then(()=>{
				change.after.ref.parent.parent.parent.parent.parent.child(`${friend}/friends/${deliveredToWhom}/last_msg/msg_status`).set(newStatus);
			});
		}
	});


});

