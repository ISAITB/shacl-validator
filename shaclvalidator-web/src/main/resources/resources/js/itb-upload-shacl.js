/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

addListener('FORM_READY', onFormReady)
addListener('ADDED_EXTERNAL_ARTIFACT_INPUT', externalShapeAdded);
addListener('REMOVED_EXTERNAL_ARTIFACT_INPUT', externalShapeRemoved);
addListener('INPUT_CONTENT_TYPE_CHANGED', inputContentTypeChanged);
addListener('VALIDATION_TYPE_CHANGED', onValidationTypeChanged);
addListener('RESULTS_LOADED', onResultsLoaded);
addListener('SUBMIT_STATUS_VALIDATED', onSubmitStatusValidated);
addListener('EXTERNAL_ARTIFACT_CONTENT_TYPE_CHANGED', externalArtifactContentTypeChanged)
addContentTypeValidator('queryType', validateQueryInputs);

function onFormReady() {
    refreshOptions();
    if (supportQuery) {
    	var selectContentTypeElement = document.getElementById("contentType");
    	if (selectContentTypeElement) {
        	selectContentTypeElement.add(createOption(labelOptionContentQuery, 'queryType'));
        	createQueryFields();
    	}
    }
}

function refreshOptions() {
    loadImportInputs();
    mergeModelsInputs();
    optionsInputs();
}

function onSubmitStatusValidated() {
    if ($('#contentType').val() == "queryType") {
        $("#query-editor-value").val(getCodeMirrorNative('#query-editor').getDoc().getValue());
    } else {
        $("#query-editor-value").val('');
    }
}

function createQueryFields() {
    // Create query fields (initially hidden)
    var fieldContent = ''
    if (supportQueryEndpoint) {
        fieldContent += ''+
           '<div class="row query-form-group">'+
               '<div class="col-sm-12 extra-query-field">'+
                   '<input type="url" placeholder="'+labelQueryEndpointInputPlaceholder+'" class="form-control" id="queryEndpoint" name="contentQueryEndpoint"">'+
               '</div>'+
           '</div>';
    }
    if (supportQueryCredentials) {
        fieldContent += ''+
           '<div class="row query-form-group-check">'+
               '<div class="container-fluid extra-query-field">'+
                   '<div class="checkbox col-sm-3 extra-query-field-left">'+
                       '<label>'+
                           '<input type="checkbox" id="queryAuthenticate" name="contentQueryAuthenticate"'+(queryCredentialsMandatory?' checked disabled':'')+'> <span>'+labelQueryAuthenticateLabel+'</span>'+
                       '</label>'+
                   '</div>'+
                   '<div id="queryCredentialsDiv" class="col-sm-9 extra-query-field '+(!queryCredentialsMandatory?'hidden':'')+'">'+
                       '<div class="container-fluid extra-query-field">'+
                           '<div class="col-sm-6">'+
                               '<input type="text" placeholder="'+labelQueryUsernameInputPlaceholder+'" class="form-control" id="queryUsername" name="contentQueryUsername">'+
                           '</div>'+
                           '<div class="col-sm-6 extra-query-field-right">'+
                               '<input type="password" placeholder="'+labelQueryPasswordInputPlaceholder+'" class="form-control" id="queryPassword" name="contentQueryPassword">'+
                           '</div>'+
                       '</div>'+
                   '</div>'+
               '</div>'+
          '</div>';
    }
    fieldContent += ''+
           '<div class="row">'+
               '<textarea id="query-editor" class="form-control"></textarea>'+
               '<input id="query-editor-value" name="contentQuery" type="hidden">'+
           '</div>';
    fieldContent = '' +
    '<div class="col-sm-12 hidden" id="queryToValidate">'+
       '<div class="container-fluid">'+
        fieldContent+
       '</div>'+
     '</div>';
    $('#fileToValidate').parent().append($(fieldContent));
    $('#queryEndpoint').on('input', checkForSubmit);
    if (supportQueryCredentials) {
        $('#queryUsername').on('input', checkForSubmit);
        $('#queryPassword').on('input', checkForSubmit);
    }
    if (!queryCredentialsMandatory) {
        $('#queryAuthenticate').on('click', toggleQueryCredentials);
    }
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
    refreshOptions();
}

function loadImportInputs() {
    $("#loadImportsCheck").prop('checked', false);	
    var validationType = getCompleteValidationType();
    if (validationType) {
    	if (loadImportsChoice[validationType] == 'REQUIRED' || loadImportsChoice[validationType] == 'OPTIONAL') {
    	    var checkByDefault = defaultLoadImports[validationType] == true;
    		$('#loadImportsDiv').removeClass('hidden');
    	    $("#loadImportsCheck").prop('checked', checkByDefault);
    	}
    	if(loadImportsChoice[validationType] == 'NONE'){
    		$('#loadImportsDiv').addClass('hidden');
    	}
    }
}

function mergeModelsInputs() {
    $("#mergeModelsCheck").prop('checked', false);
    var validationType = getCompleteValidationType();
    if (validationType) {
    	if (mergeModelsChoice[validationType] == 'REQUIRED' || mergeModelsChoice[validationType] == 'OPTIONAL') {
    	    var checkByDefault = defaultMergeModels[validationType] == true;
    		$('#mergeModelsDiv').removeClass('hidden');
    	    $("#mergeModelsCheck").prop('checked', checkByDefault);
    	}
    	if(mergeModelsChoice[validationType] == 'NONE'){
    		$('#mergeModelsDiv').addClass('hidden');
    	}
    }
}

function optionsInputs() {
  var validationType = getCompleteValidationType();
  if (validationType) {
    if (loadImportsChoice[validationType] == 'REQUIRED' || loadImportsChoice[validationType] == 'OPTIONAL' || mergeModelsChoice[validationType] == 'REQUIRED' || mergeModelsChoice[validationType] == 'OPTIONAL') {
      $('#optionsDiv').removeClass('hidden');
    } else {
      $('#optionsDiv').addClass('hidden');
    }
  }
}

function inputContentTypeChanged() {
	var type = $('#contentType').val();
	// Adapt content syntax selection
    $('#contentSyntaxType option[value="empty"]').remove();
    if (type == 'uriType') {
        $('#contentSyntaxType').prepend('<option value="empty" selected="selected">'+labelContextSyntaxDefaultForUri+'</option>');
    } else if (type == 'fileType') {
        $('#contentSyntaxType').prepend('<option value="empty" selected="selected">'+labelContextSyntaxDefault+'</option>');
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

function externalArtifactContentTypeChanged(event, info) {
    if (info) {
    	var type = $('#contentType-'+info.elementId).val();
        // Adapt content syntax selection
        $('#contentSyntaxType-'+info.elementId+' option[value="empty"]').remove();
        if (type == 'uriType') {
            $('#contentSyntaxType-'+info.elementId+'').prepend('<option value="empty" selected="selected">'+labelContextSyntaxDefaultForUri+'</option>');
        } else if (type == 'fileType') {
            $('#contentSyntaxType-'+info.elementId+'').prepend('<option value="empty" selected="selected">'+labelContextSyntaxDefault+'</option>');
        }
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
    var stringInput = $('#stringToValidate-external_default-'+eventData.elementIndex);
    fileTextInput.removeClass('col-sm-11').addClass('col-sm-8');
    uriInput.removeClass('col-sm-11').addClass('col-sm-8');
    stringInput.addClass('external-shape-code-editor');

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

function downloadShapes(syntaxType) {
    doDownload('shapesType', syntaxType, 'downloadShapesButton');
}

function downloadReport(syntaxType) {
    doDownload('reportType', syntaxType, 'downloadReportButton');
}

function downloadContent(syntaxType) {
    doDownload('contentType', syntaxType, 'downloadInputButton');
}

function doDownload(downloadType, syntaxType, buttonId) {
    $('#'+buttonId).prop('disabled', true);
    $('#'+buttonId+'Spinner').removeClass('hidden');
    var xhr = new XMLHttpRequest();
    xhr.open("GET", "report?id="+encodeURIComponent(resultReportID)+'&type='+encodeURIComponent(downloadType)+'&syntax='+encodeURIComponent(syntaxType), true);
    xhr.setRequestHeader("X-Requested-With", "XmlHttpRequest");
    xhr.onreadystatechange = function (){
        if (xhr.readyState === 2) {
            if (xhr.status == 200) {
                xhr.responseType = 'blob'
            } else {
                xhr.responseType = 'text'
            }
        }
        if (xhr.readyState === 4) {
            if (xhr.status === 200) {
                clearMessages();
                var responseData;
                var fileName = xhr.getResponseHeader('Content-Disposition').split("filename=")[1];
                if (syntaxType === "pdfType") {
                    responseData = new Blob([xhr.response], {type: "application/octet-stream"});
                } else {
                    responseData = new Blob([xhr.response], {type: syntaxType});
                }
                saveAs(responseData, fileName);
            } else {
                raiseAlert(labelDownloadErrorMessage, isFinal);
            }
            $('#'+buttonId).prop('disabled', false);
            $('#'+buttonId+'Spinner').addClass('hidden');
        }
    }
    xhr.send(null);
}

function createOption(text, value){
	var option = document.createElement("option");
	option.text = text;
	option.value = value;
	return option;
}

function onResultsLoaded(event, data) {
    var reportId = data.data.reportId
    // Register cleanup
    window.addEventListener("beforeunload", function() {
        if (navigator.sendBeacon) {
            // Modern browsers
            navigator.sendBeacon("delete/"+reportId)
        } else {
            // IE
            var xhr = new XMLHttpRequest();
            xhr.open("POST", "delete/"+reportId, true);
            xhr.setRequestHeader("X-Requested-With", "XmlHttpRequest");
            xhr.send(null);
        }
    });
    // Register event listeners
    $(".downloadReport").off().on("click", function(event) { downloadReport($(this).attr("data-report-type")); event.preventDefault(); });
    $(".downloadContent").off().on("click", function(event) { downloadContent($(this).attr("data-content-type")); event.preventDefault(); });
    if (data.data.hideDownloadShapes) {
      $("#downloadShapesButtonDiv").hide();
    } else {
      $(".downloadShapes").off().on("click", function(event) { downloadShapes($(this).attr("data-content-type")); event.preventDefault(); });
    }
}