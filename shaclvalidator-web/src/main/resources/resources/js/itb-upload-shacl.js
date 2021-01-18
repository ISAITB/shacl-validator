addListener('FORM_READY', loadImportInputs)
addListener('ADDED_EXTERNAL_ARTIFACT_INPUT', externalShapeAdded);
addListener('REMOVED_EXTERNAL_ARTIFACT_INPUT', externalShapeRemoved);
addListener('INPUT_CONTENT_TYPE_CHANGED', inputContentTypeChanged);
addListener('VALIDATION_TYPE_CHANGED', loadImportInputs);

function loadImportInputs(){
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
    if (type == 'stringType') {
        $('#contentSyntaxType option[value="empty"]').remove();
    } else {
        if ($('#contentSyntaxType option[value="empty"]').length == 0) {
            $('#contentSyntaxType').prepend('<option value="empty" selected="selected"></option>');
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

function downloadResult() {
	var type = $('#downloadType').val();
	var syntaxType = $('#downloadSyntaxType').val();
	if (syntaxType != 'notSelect') {
        var selectedIndex = document.getElementById("downloadSyntaxType").selectedIndex;
        var selectedSyntax = document.getElementById("downloadSyntaxType")[selectedIndex].text;
        $('#resultFormId').val(resultReportID);
        $('#resultFormType').val(type);
        $('#resultFormSyntax').val(syntaxType.replace("/", "_"));
        $('#resultForm').submit();
	}
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