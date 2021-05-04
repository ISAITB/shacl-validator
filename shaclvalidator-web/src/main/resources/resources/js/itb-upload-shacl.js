addListener('FORM_READY', onFormReady)
addListener('ADDED_EXTERNAL_ARTIFACT_INPUT', externalShapeAdded);
addListener('REMOVED_EXTERNAL_ARTIFACT_INPUT', externalShapeRemoved);
addListener('INPUT_CONTENT_TYPE_CHANGED', inputContentTypeChanged);
addListener('VALIDATION_TYPE_CHANGED', onValidationTypeChanged);
addContentTypeValidator('queryType', validateQueryInputs);

function onFormReady() {
    loadImportInputs();
    if (supportQuery) {
    	var selectContentTypeElement = document.getElementById("contentType");
    	selectContentTypeElement.add(createOption(labelOptionContentQuery, 'queryType'));
    	createQueryFields();
    }
}

function createQueryFields() {
    // Create query fields (initially hidden)
    var fieldContent = ''
    if (supportQueryEndpoint) {
        fieldContent += ''+
           '<div class="row query-form-group">'+
               '<div class="col-sm-12 extra-query-field">'+
                   '<input type="url" placeholder="'+labelQueryEndpointInputPlaceholder+'" class="form-control" id="queryEndpoint" name="contentQueryEndpoint" oninput="checkForSubmit()">'+
               '</div>'+
           '</div>';
    }
    if (supportQueryCredentials) {
        fieldContent += ''+
           '<div class="row query-form-group-check">'+
               '<div class="container-fluid extra-query-field">'+
                   '<div class="checkbox col-sm-3 extra-query-field-left">'+
                       '<label>'+
                           '<input type="checkbox" id="queryAuthenticate" name="contentQueryAuthenticate" '+(queryCredentialsMandatory?'checked disabled':'onclick="toggleQueryCredentials()"')+'> <span>'+labelQueryAuthenticateLabel+'</span>'+
                       '</label>'+
                   '</div>'+
                   '<div id="queryCredentialsDiv" class="col-sm-9 extra-query-field '+(!queryCredentialsMandatory?'hidden':'')+'">'+
                       '<div class="container-fluid extra-query-field">'+
                           '<div class="col-sm-6">'+
                               '<input type="text" placeholder="'+labelQueryUsernameInputPlaceholder+'" class="form-control" id="queryUsername" name="contentQueryUsername" oninput="checkForSubmit()">'+
                           '</div>'+
                           '<div class="col-sm-6 extra-query-field-right">'+
                               '<input type="password" placeholder="'+labelQueryPasswordInputPlaceholder+'" class="form-control" id="queryPassword" name="contentQueryPassword" oninput="checkForSubmit()">'+
                           '</div>'+
                       '</div>'+
                   '</div>'+
               '</div>'+
          '</div>';
    }
    fieldContent += ''+
           '<div class="row">'+
               '<textarea id="query-editor" name="contentQuery" class="form-control"></textarea>'+
           '</div>';
    fieldContent = '' +
    '<div class="col-sm-12 hidden" id="queryToValidate">'+
       '<div class="container-fluid">'+
        fieldContent+
       '</div>'+
     '</div>';
    $('#fileToValidate').parent().append($(fieldContent));
    CodeMirror.fromTextArea(document.getElementById('query-editor'), {
        lineNumbers: true
    }).on('change', function(){
        checkForSubmit();
    });
}

function toggleQueryCredentials() {
    var checked = $("#queryAuthenticate").is(":checked");
    if (checked) {
        $('#queryCredentialsDiv').removeClass('hidden');
    } else {
        $('#queryCredentialsDiv').addClass('hidden');
    }
    checkForSubmit();
}

function validateQueryInputs() {
    var valid = false;
    if (supportQuery) {
        var endpointOk = false;
        if (supportQueryEndpoint) {
            endpointOk = $('#queryEndpoint').val()?true:false;
        } else {
            endpointOk = true;
        }
        var credentialsOk = false
        if (supportQueryCredentials) {
            if ($("#queryAuthenticate").is(":checked")) {
                var hasUsername = $('#queryUsername').val()?true:false;
                var hasPassword = $('#queryPassword').val()?true:false;
                credentialsOk = hasUsername && hasPassword;
            } else {
                credentialsOk = true;
            }
        } else {
            credentialsOk = true;
        }
		var queryValue = getCodeMirrorNative('#query-editor').getDoc().getValue();
		if (queryValue && endpointOk && credentialsOk) {
		    valid = true;
		}
    }
    return valid;
}

function onValidationTypeChanged() {
    loadImportInputs();
}

function loadImportInputs() {
    $("#loadImportsCheck").prop('checked', false);	
    var validationType = getCompleteValidationType();
    if (validationType) {
    	if(loadImportsArtifacts[validationType] == 'REQUIRED' || loadImportsArtifacts[validationType] == 'OPTIONAL'){
    		$('#loadImportsDiv').removeClass('hidden');
    	    $("#loadImportsCheck").prop('checked', false);
    	}
    	if(loadImportsArtifacts[validationType] == 'NONE'){
    		$('#loadImportsDiv').addClass('hidden');
    	}
    }
}

function inputContentTypeChanged() {
	var type = $('#contentType').val();
	// Adapt content syntax selection
    if (type == 'stringType') {
        $('#contentSyntaxType option[value="empty"]').remove();
    } else {
        if ($('#contentSyntaxType option[value="empty"]').length == 0) {
            $('#contentSyntaxType').prepend('<option value="empty" selected="selected"></option>');
        }
    }
	// Show or hide query fields
    if (type == 'queryType') {
        $('#contentSyntaxTypeDiv').addClass('hidden');
        $('#fileToValidate').addClass('hidden');
        $('#uriToValidate').addClass('hidden');
        $('#stringToValidate').addClass('hidden');
        $('#queryToValidate').removeClass('hidden');
		setTimeout(function() {
            var codeMirror = getCodeMirrorNative('#query-editor')
            codeMirror.refresh();
		}, 0);
    } else {
        $('#queryToValidate').addClass('hidden');
        $('#contentSyntaxTypeDiv').removeClass('hidden');
    }
}

function externalShapeRemoved(eventType, eventData) {
    var supportType = getExternalArtifactSupport('default');
    if (supportType == 'required') {
        var currentInputRows = $('.externalDiv_default').length
        if (currentInputRows == 1) {
            $('.contentSyntaxTypeDiv-external').removeClass('col-sm-3').addClass('col-sm-4');
        }
    }
}

function externalShapeAdded(eventType, eventData) {
    var cols = 3;
    var supportType = getExternalArtifactSupport('default');
    if (supportType == 'required') {
        var currentInputRows = $('.externalDiv_default').length
        if (currentInputRows == 1) {
            cols = 4;
        } else if (currentInputRows > 1) {
            $('.contentSyntaxTypeDiv-external').removeClass('col-sm-4').addClass('col-sm-3');
        }
    }
    var fileTextInput = $('#fileToValidate-class-external_default-'+eventData.elementIndex);
    var uriInput = $('#uriToValidate-external_default-'+eventData.elementIndex);
    var fileInput = $('#inputFile-external_default-'+eventData.elementIndex);
    fileTextInput.removeClass('col-sm-11').addClass('col-sm-8');
    uriInput.removeClass('col-sm-11').addClass('col-sm-8');

    var selectElementDiv = $('<div class="contentSyntaxTypeDiv-external col-sm-'+cols+'"></div>');
    var selectElement = $('<select class="form-control" id="contentSyntaxType-external_default-'+eventData.elementIndex+'" name="contentSyntaxType-external_default"></select>');
    $('#contentSyntaxType option').each(function() {
        selectElement.append($('<option value="'+$(this).val()+'">'+$(this).text()+'</option>'))
    });
    selectElementDiv.append(selectElement).insertAfter(fileInput);
}

var resultReportID;

function getReportData(reportID) {
	resultReportID = reportID;
}

function downloadResult () {
	var type = $('#downloadType').val();
	var syntaxType = $('#downloadSyntaxType').val();
	var resultFormId = $('#resultFormId').val(resultReportID).val();
    var resultFormType = $('#resultFormType').val(type).val();
    var resultFormSyntax = $('#resultFormSyntax').val(syntaxType.replace("/", "_")).val();
	if (syntaxType != 'notSelect') {
        var selectedIndex = document.getElementById("downloadSyntaxType").selectedIndex;
        var selectedSyntax = document.getElementById("downloadSyntaxType")[selectedIndex].text;
        
        var xhr = new XMLHttpRequest();
        var params = '?id='+encodeURIComponent(resultFormId)+'&type='+encodeURIComponent(resultFormType)+'&syntax='+encodeURIComponent(resultFormSyntax);

        xhr.open("GET", "report"+params, true);
        xhr.setRequestHeader("X-Requested-With", "XmlHttpRequest");
        xhr.onreadystatechange = function (){
        	if(xhr.readyState === 2){
        		if (xhr.status == 200) {
        			xhr.responseType = 'blob'
        		} else {
        			xhr.responseType = 'text'
        		}
        	}
        	if(xhr.readyState === 4){
        		if(xhr.status === 200){
        			$(".alert.alert-danger.ajax-error").remove();
        			var responseData;
        			var fileName = xhr.getResponseHeader('Content-Disposition').split("filename=")[1];
        			if(resultFormSyntax === "pdfType"){
        				responseData = new Blob([xhr.response], {type: "application/octet-stream"});
        			}else{	
        				responseData = new Blob([xhr.response], {type: syntaxType});
        			}
        			saveAs(responseData, fileName);
        		}else{
        			raiseAlert(getAjaxErrorMessage());
        		}
        	}
        }
        xhr.send(null);
	}
}
function getAjaxErrorMessage(){
	var selectedDownloadType = $("#downloadType option:selected").text();
	var selectedDownloadSyntaxType = $("#downloadSyntaxType option:selected").text();
	return "Error downloading " + selectedDownloadType + " in " + selectedDownloadSyntaxType + " format.";
}
function raiseAlert(errorMessage){
	$(".alert.alert-danger.ajax-error").remove();
	const alertDiv = $("<div class='alert alert-danger ajax-error'></div>");
	alertDiv.text(errorMessage);
	alertDiv.insertAfter(".view-section-input");
}

function downloadTypeChange(){
	var dType = $('#downloadType').val();
	var dSyntaxType = document.getElementById("downloadSyntaxType");
    if (reportItemCount <= reportItemDetailMax) {
        dSyntaxType = removeOption(dSyntaxType, 'pdfType');
        dSyntaxType = removeOption(dSyntaxType, 'notSelect');
    }
	if(dType == "reportType"){
	    if (reportItemCount <= reportItemDetailMax) {
            dSyntaxType.add(createOption("PDF", "pdfType"),0);
            dSyntaxType.selectedIndex = 0;
            dSyntaxType.add(createOption("-----------------------", "notSelect"),1);
	    }
	}
}

function removeOption(options, value){
	for (var i=0; i<options.length; i++){
		 if (options[i].value == value){
			 options.remove(i);
		 }
	}
	
	return options;
}

function createOption(text, value){
	var option = document.createElement("option");
	option.text = text;
	option.value = value;
	return option;
}