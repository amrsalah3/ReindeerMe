const functions = require('firebase-functions');
const admin = require('firebase-admin');

exports.changeAvatar = functions.database
.ref('users/{user}/ppurl')
.onUpdate((change, context) => {
	
	const user = context.params.user;
	const avatarUrl = change.after.val();

	admin.database().ref(`/users/${user}/friends`).once("value", function(snapshot) {
		snapshot.forEach(function(childSnapshot) {
			const friendId = childSnapshot.key;
			snapshot.ref.parent.parent.child(friendId).child("friends").child(user).child("ppurl").set(avatarUrl);
		});
		
	});

});

