getPropreties = {
	url: "http://localhost:9090/analytics-store/rest/"
	//url: "http://dev:8091/analytics-store/rest/"
}

function sessionStorage() {
	if(typeof(Storage) !== "undefined") {
		console.log("yes");
		localStorage.setItem("email", "igor.souza@idiro.com");
	} else {
		console.log("no");
	}
}

function sessionStorageToken(token) {
	if(typeof(Storage) !== "undefined") {
		localStorage.setItem("token", token);
	}
}

function sessionStorageEmail(email) {
	if(typeof(Storage) !== "undefined") {
		localStorage.setItem("email", email);
	}
}

function getsessionToken() {
	if(typeof(Storage) !== "undefined") {
		return localStorage.getItem("token");
	}
}

function getsessionEmail() {
	if(typeof(Storage) !== "undefined") {
		return localStorage.getItem("email");
	}
}

function makeBaseAuth(user, password) {
    var tok = user + ':' + password;
    var hash = btoa(tok);
    return 'Basic' + hash;
}

function getUrlParameter(sParam) {
    var sPageURL = decodeURIComponent(window.location.search.substring(1)),
        sURLVariables = sPageURL.split('&'),
        sParameterName,
        i;

    for (i = 0; i < sURLVariables.length; i++) {
        sParameterName = sURLVariables[i].split('=');

        if (sParameterName[0] === sParam) {
            return sParameterName[1] === undefined ? true : sParameterName[1];
        }
    }
}

function login() {

	jQuery.ajax({
		method: "POST",
		dataType: "json",
		contentType: "application/json; charset=utf-8",
		url: getPropreties.url+"authentication",
		beforeSend: function (xhr) {
			xhr.setRequestHeader('Authorization', makeBaseAuth(jQuery("#email").val(), jQuery("#password").val()));
		},
		complete: function(xhr, textStatus) {
			if(textStatus == 'error'){
				alert("Sorry, Password incorrect for given User");
			}else{
				sessionStorageEmail(jQuery("#email").val());
				sessionStorageToken(xhr.responseText);
				$('#myModal').modal('hide');
				checkMenu();
			}
		},
		error: function (request, status, error) {
			if (request.status == 401) {
				alert("Sorry, your session has expired. Please login again to continue");
			}
		}
	});
}

function register() {

	jQuery.ajax({
		method: "POST",
		dataType: "json",
		contentType: "application/json; charset=utf-8",
		url: getPropreties.url+"createnewuser",
		data: JSON.stringify({ firstName: jQuery("#firstName").val(), lastName: jQuery("#lastName").val(), email: jQuery("#emailRegister").val(), company: jQuery("#company").val(), password: jQuery("#password").val() }),
		complete: function(xhr, textStatus) {
			
		},
		error: function (request, status, error) {
			if (request.status == 401) {
				alert("Sorry, your session has expired. Please login again to continue");
			}
		}
	}).then(function(data) {
		alert(data.error);
		$('#myModalNewUser').modal('hide');
	});

}

function downloadModule(value){
	download(getPropreties.url+'download/downloadModel', JSON.stringify({ id: value }));
}

function download(url, data){

	jQuery.ajax({
		method: "POST",
		contentType: "application/json; charset=utf-8",
		url: url,
		data: data,
		beforeSend: function (xhr) {
			xhr.setRequestHeader('Authorization', getsessionToken());
		},
		success: function (response, status, xhr) {

			var filename = "";
			var disposition = xhr.getResponseHeader('Content-Disposition');

			if (disposition && disposition.indexOf('attachment') !== -1) {
				var filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
				var matches = filenameRegex.exec(disposition);
				if (matches !== null && matches[1]) {
					filename = matches[1].replace(/['"]/g, '');
				}
			}
			var a = document.createElement("a");
			a.href = "data:application/octet-stream;charset=utf-8;base64,"+response;
			a.download = filename;
			document.body.appendChild(a);
			a.click();

		},
		error: function (request, status, error) {
			if (request.status == 401) {
				alert("Sorry, your session has expired. Please login again to continue");
			}
		}

	}).then(function(data) {
		
	});

}

function checkMenu() {

	if(getsessionToken() !== null){
		$(".container").load();
		$("#signupMenu").fadeOut("fast");
		$("#loginMenu").fadeOut("fast");
		$("#signOut").fadeIn("fast");
		$("#newLoginMenu").fadeIn("fast");
		$("#userNameEmail").text(getsessionEmail());
	}else{
		$(".container").load();
		$("#signupMenu").fadeIn("fast");
		$("#loginMenu").fadeIn("fast");
		$("#signOut").fadeOut("fast");
		$("#newLoginMenu").fadeOut("fast");
	}

}

function signout() {

	jQuery.ajax({
		method: "POST",
		dataType: "json",
		contentType: "application/json; charset=utf-8",
		url: getPropreties.url+"signout",
		//data: ,
		beforeSend: function (xhr) {
			xhr.setRequestHeader('Authorization', getsessionToken());
		},
		complete: function(xhr, textStatus) {
        		console.log(xhr.responseText);
			localStorage.removeItem("token");
			localStorage.removeItem("email");
			window.location.href = "index.html";
		},
		error: function (request, status, error) {
			if (request.status == 401) {
				alert("Sorry, your session has expired. Please login again to continue");
			}
		}
	});

}

function validadelogInForm(){

var empty = true;
    $('#logInForm input').each(function(){
        if ($(this).val() === ""){
		if($(this).next("span").hasClass("validForm")){
			$(this).next("span").remove();
		}
		$(this).after( "<span class='validForm' >&nbsp;&nbsp;This field is required</span>" );
		$(this).attr('style', 'border: 1px solid red;');
		empty = false;
	}else{

		if($(this).attr("id") == "email"){
			if(!isValidEmailAddress($(this).val())){
				if($(this).next("span").hasClass("validForm")){
					$(this).next("span").remove();
				}
				$(this).after( "<span class='validForm' >&nbsp;&nbsp;Please enter a valid email</span>" );
				$(this).attr('style', 'border: 1px solid red;');
				empty = false;
			}
		}else{
			$(this).removeAttr( 'style' );
			$(this).attr('style', 'width: 420px;');
			$(this).next("span").remove();
		}

	}
    });

	if(empty){
		login();
	}
}

function validadeRegisterForm(){

	var empty = true;
	$('#registerForm input').each(function(){
		if ($(this).val() === ""){
			if($(this).next("span").hasClass("validForm")){
				$(this).next("span").remove();
			}
			$(this).after("<span class='validForm' >&nbsp;&nbsp;This field is required</span>");
			$(this).attr('style', 'border: 1px solid red;');
			empty = false;
		}else{
			if($(this).attr("id") == "emailRegister"){
				if(!isValidEmailAddress($(this).val())){
					if($(this).next("span").hasClass("validForm")){
						$(this).next("span").remove();
					}
					$(this).after("<span class='validForm' >&nbsp;&nbsp;Please enter a valid email</span>");
					$(this).attr('style', 'border: 1px solid red;');
					empty = false;
				}
			}else{
				$(this).removeAttr( 'style' );
				$(this).attr('style', 'width: 420px;');
				$(this).next("span").remove();
			}
		}
	});

	if(empty){
		register();
	}

}

function isValidEmailAddress(emailAddress) {
	var pattern = new RegExp(/^([\w-\.]+@([\w-]+\.)+[\w-]{2,4})?$/);
	return pattern.test(emailAddress);
}

function validadeRequestKeyForm(version, installationName, mac, email){

	var empty = true;
	$('#requestKeyForm input').each(function(){
		if ($(this).val() === ""){
			if($(this).next("span").hasClass("validForm")){
				$(this).next("span").remove();
			}
			$(this).after("<span class='validForm' >&nbsp;&nbsp;This field is required</span>");
			$(this).attr('style', 'border: 1px solid red;');
			empty = false;
		}else{
			$(this).removeAttr('style');
			$(this).attr('style', 'width: 420px;');
			$(this).next("span").remove();
		}
	});

	if(empty){
		requestKey(version, installationName, mac, email);
	}

}

function requestKey(version, installationName, mac, email){

	jQuery.ajax({
		method: "POST",
		dataType: "json",
		contentType: "application/json; charset=utf-8",
		url: getPropreties.url+"licensekey",
		data: JSON.stringify({ version: version, mac: mac, installationName: installationName, email: email }),
		beforeSend: function (xhr) {
			xhr.setRequestHeader('Authorization', getsessionToken());
		},
		complete: function(xhr, textStatus) {
			console.log(xhr.responseText);
		},
		error: function (request, status, error) {
			if (request.status == 401) {
				alert("Sorry, your session has expired. Please login again to continue");
			}
		}
	}).then(function(data) {
		if(data.error != ""){
			alert(data.error);
		}
	});

}

function validadeRequestModuleKeyForm(idk, idm, type, name){
	requestModuleKey(idk, idm, type, name);
}

function requestModuleKey(idk, idm, type, name){

	jQuery.ajax({
		method: "POST",
		dataType: "json",
		contentType: "application/json; charset=utf-8",
		url: getPropreties.url+"createModuleKey",
		data: JSON.stringify({ idk: idk, idm: idm, email: getsessionEmail(), type: type, name: name }),
		beforeSend: function (xhr) {
			xhr.setRequestHeader('Authorization', getsessionToken());
		},
		complete: function(xhr, textStatus) {
			console.log(xhr.responseText);
		},
		error: function (request, status, error) {
			if (request.status == 401) {
				alert("Sorry, your session has expired. Please login again to continue");
			}
		}
	}).then(function(data) {
		if(data.error != ""){
			alert(data.error);
		}else{
			alert('success');
		}
	});

}

function cleamForm(){

	$('input').each(function(){

		if($(this).attr("type") == "text" || $(this).attr("type") == "password"){
			$(this).removeAttr('style');
			$(this).attr('style', 'width: 420px;');
			$(this).val("");
			if($(this).next("span").hasClass("validForm")){
				$(this).next("span").remove();
			}
		}

	});

}

function enableEnterKey(e, id){
    var key;
    if(window.event)
         key = window.event.keyCode;
    else
         key = e.keyCode;

    if (key == 13){
    	e.preventDefault();
    	jQuery("[id$='"+id+"']").click();
    }
    return (key != 13);
}

function removeDuplicate(items,propertyName){
	var newArr = [];
	var lookup = {};
	for (var i in items) {
	    lookup[items[i][propertyName]] = items[i];
	}
	for (i in lookup) {
	    newArr.push(lookup[i]);
	}
	return newArr;
}

function validadeRequestNewPasswordForm(){

	var empty = true;

	var email = $("#emailRequestNewPassword");

	if (email.val() === ""){
		if(email.next("span").hasClass("validForm")){
				email.next("span").remove();
		}
		email.after("<span class='validForm' >&nbsp;&nbsp;This field is required</span>");
		email.attr('style', 'border: 1px solid red;');
		empty = false;
	}else{
		if(!isValidEmailAddress(email.val())){
			if(email.next("span").hasClass("validForm")){
				email.next("span").remove();
			}
			email.after( "<span class='validForm' >&nbsp;&nbsp;Please enter a valid email</span>" );
			email.attr('style', 'border: 1px solid red;');
			empty = false;
		}else{
			email.removeAttr('style');
			email.attr('style', 'width: 420px;');
			email.next("span").remove();
		}
	}

	if(empty){
		requestNewPasswordForm(email.val());
	}

}

function requestNewPasswordForm(email){

	jQuery.ajax({
		method: "POST",
		dataType: "json",
		contentType: "application/json; charset=utf-8",
		url: getPropreties.url+"requestNewPassword",
		data: JSON.stringify({ email: email }),
		complete: function(xhr, textStatus) {
			
		},
		error: function (request, status, error) {
			if (request.status == 401) {
				alert("Sorry, your session has expired. Please login again to continue");
			}
		}
	}).then(function(data) {
		alert(data.error);
		$('#requestNewPassword').modal('hide');
	});

}