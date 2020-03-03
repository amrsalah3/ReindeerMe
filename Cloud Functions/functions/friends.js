const functions = require('firebase-functions');
const admin = require('firebase-admin');

exports.sendRequest = functions.database
	.ref('users/{reqSender}/outpendingfriendreq/{requestedUser}')
	.onCreate((snapshot, context)=>{
		const requestSenderId = context.params.reqSender;
		const requestedUser = context.params.requestedUser;
		// Get Sender Name, then send request
		admin.database().ref(`/users/${requestSenderId}/name`).once("value", function (nameSnap) {
			const requestSenderName = nameSnap.val();
			snapshot.ref.parent.parent.parent.child(requestedUser).child("inpendingfriendreq").child(requestSenderId).set(requestSenderName);
		});
	});
		
		
exports.acceptFriend = functions.database
	.ref('users/{acceptor}/friends/{newFri}')
	.onCreate((snapshot, context)=>{
		const acceptorId = context.params.acceptor;
		const friendId = context.params.newFri;
		const receiverId = context.params.newFri;
				
		admin.database().ref(`/users/${friendId}/friends/${acceptorId}`).once("value", function (checksnap) {
			if (checksnap.val() != null) {return null;} // Break the function if there is no outpending req 

			// Get the info (name) of the acceptor, then save him as a friend to the accepted user
			admin.database().ref(`/users/${acceptorId}`).once("value", function (datasnap) {
				const acceptorName = datasnap.child("name").val();
				const ppurl = datasnap.child("ppurl").val();
				const newFriend = {
					"name": acceptorName,
					"ppurl": ppurl
				}
				snapshot.ref.parent.parent.parent.child(friendId).child("friends").child(acceptorId).set(newFriend).then(()=>{
					admin.database().ref(`/users/${receiverId}/registration_token`).once("value", function(tokenSnapshot) {
						// Send results to the accepted user
						var regToken = tokenSnapshot.val();
						var message = {
							token : regToken,
							data : {
								fn_call_code: "accept_friend",
								friend_id: acceptorId,
								friend_name: acceptorName,
								receiver_id: receiverId
							}
							
						};  
						// Now sending notification to the receiver
						return admin.messaging().send(message)
						.then((response) => {
						// Response is a message ID string.
						console.log('Successfully sent notification about acceptance.', response);
						})
						.catch((error) => {
						console.log('Error sending notification about acceptance: ', error);
						});
					});
				});
				
				
			});

		});
		
	});
	
	
exports.deleteFriend = functions.database
	.ref('users/{deleter}/friends/{oldFri}')
	.onDelete((snapshot, context)=>{
		const deleterId = context.params.deleter;
		const deletedFriendId = context.params.oldFri;
		const receiverId = context.params.oldFri;

		admin.database().ref(`/users/${deletedFriendId}/friends/${deleterId}`).once("value", function (checksnap) {
			if (checksnap.val() == null) {return null;} // Break the function if he is already deleted	

			// Now delete him from the other side
			snapshot.ref.parent.parent.parent.child(deletedFriendId).child("friends").child(deleterId).set(null).then(()=>{
				admin.database().ref(`/users/${receiverId}/registration_token`).once("value", function(tokenSnapshot) {
					// Send results to the accepted user
					var regToken = tokenSnapshot.val();
					var message = {
						token : regToken,
						data : {
							fn_call_code: "delete_friend",
							old_friend_id: deleterId,
							receiver_id: receiverId
						}
						
					};  
					// Now sending notification to the receiver
					return admin.messaging().send(message)
					.then((response) => {
					// Response is a message ID string.
					console.log('Successfully sent notification about deletion.', response);
					})
					.catch((error) => {
					console.log('Error sending notification about deletion: ', error);
					});
				});
			}); 
		});	
	});	
	
	
exports.declineRequest = functions.database
	.ref('users/{decliner}/inpendingfriendreq/{reqFri}')
	.onDelete((snapshot, context)=>{
		const declinerId = context.params.decliner;
		const declinedFriId = context.params.reqFri;

		admin.database().ref(`/users/${declinedFriId}/outpendingfriendreq/${declinerId}`).once("value", function (checksnap) {
			if (checksnap.val() == null) {return null;} // Break the function if he is already deleted	
			
			// Now delete him from the other side outpending req
			return snapshot.ref.parent.parent.parent.child(declinedFriId).child("outpendingfriendreq").child(declinerId).set(null);
		});	
		
	});		