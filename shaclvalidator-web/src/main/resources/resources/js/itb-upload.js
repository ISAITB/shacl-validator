function contentTypeChanged(){
	var type = $('#contentType').val();
	$('#inputFileSubmit').prop('disabled', true);
	
	if(type == "uriType"){
		addEmptyOption();
		$("#uriToValidate").removeClass('hidden');
		$("#fileToValidate").addClass('hidden');
		$("#stringToValidate").addClass('hidden');
	}
	if(type == "fileType"){
		addEmptyOption();
		$("#fileToValidate").removeClass('hidden');
		$("#uriToValidate").addClass('hidden');
		$("#stringToValidate").addClass('hidden');
	}
	if(type == "stringType"){	
		removeEmptyOption();	
		$("#stringToValidate").removeClass('hidden');
		$("#uriToValidate").addClass('hidden');
		$("#fileToValidate").addClass('hidden');
	}
}

function removeEmptyOption(){
	var contentType = document.getElementById("contentSyntaxType");
	
	for (var i=0; i<contentType.length; i++){
		 if (contentType[i].value == 'empty'){
			 contentType.remove(i);
		 }
	}
}

function addEmptyOption(){
	var contentType = document.getElementById("contentSyntaxType");
	var exist = false;
	
	for (var i=0; i<contentType.length; i++){
		 if (contentType[i].value == 'empty'){
			 exist = true;
		 }
	}
	
	if(exist == false){
		var option = document.createElement("option");
		option.text = "";
		option.value = "empty";
		
		contentType.add(option,0);
		contentType.selectedIndex = 0;
	}
}

function fileInputChanged() {
	if($('#contentType').val()=="fileType"){
		$('#inputFileName').val($('#inputFile')[0].files[0].name);
	}
	checkForSubmit();
}
function fileInputChangedShapes(type){
	$("#inputFileName-"+type+"").val($("#inputFile-"+type+"")[0].files[0].name);
}
function contentSyntaxChanged() {
	checkForSubmit();
}
function checkForSubmit() {
	var type = $('#contentType').val();
	$('#inputFileSubmit').prop('disabled', true);
	
	if(type == "fileType"){
		var inputFile = $("#inputFileName");
		$('#inputFileSubmit').prop('disabled', (inputFile.val())?false:true);
	}
	if(type == "uriType"){
		var uriInput = $("#uri");
		$('#inputFileSubmit').prop('disabled', (uriInput.val())?false:true);		
	}
	if(type == "stringType"){
		var stringType = getCodeMirrorNative('#text-editor').getDoc();	
		var contentType = $("#contentSyntaxType");
		
		$('#inputFileSubmit').prop('disabled', (stringType.getValue() && (!contentType.length || contentType.val()))?false:true);		
	}
}

function getCodeMirrorNative(target) {
    var _target = target;
    if (typeof _target === 'string') {
        _target = document.querySelector(_target);
    }
    if (_target === null || !_target.tagName === undefined) {
        throw new Error('Element does not reference a CodeMirror instance.');
    }
    
    if (_target.className.indexOf('CodeMirror') > -1) {
        return _target.CodeMirror;
    }

    if (_target.tagName === 'TEXTAREA') {
        return _target.nextSibling.CodeMirror;
    }
    
    return null;
};

function triggerFileUpload() {
	$('#inputFile').click();
}
function triggerFileUploadShapes(elementId) {
    $("#"+elementId).click();
}
function uploadFile() {
	waitingDialog.show('Validating input', {dialogSize: 'm'});
	return true;
}
function removeElement(elementId) {
    $("#"+elementId).remove();
}

function contentTypeChangedShapes(elementId){
	var type = $('#contentType-'+elementId).val();
	
	if(type == "uriType"){
		$("#uriToValidate-"+elementId).removeClass('hidden');
		$("#fileToValidate-"+elementId).addClass('hidden');
	}
	if(type == "fileType"){
		$("#fileToValidate-"+elementId).removeClass('hidden');
		$("#uriToValidate-"+elementId).addClass('hidden');
	}
}

function addElement(type) {
    var elements = $("."+type+"Div").length;
    var elementId = type+"-"+elements;
    var options = $( "#contentSyntaxType" ).html();

    $("<div class='row form-group "+type+"Div' id='"+elementId+"'>" +
    	"<div class='col-sm-2'>"+
			"<select class='form-control' id='contentType-"+elementId+"' name='contentType-"+type+"' onchange='contentTypeChangedShapes(\""+elementId+"\")'>"+
				"<option value='fileType' selected='true'>File</option>"+
				"<option value='uriType'>URI</option>"+
		    "</select>"+
		"</div>"+
		"<div class='col-sm-10'>" +
		    "<div class='row'>" +
                "<div class='col-md-8 col-sm-7'>" +
                    "<div class='input-group' id='fileToValidate-"+elementId+"'>" +
                        "<div class='input-group-btn'>" +
                            "<button class='btn btn-default' type='button' onclick='triggerFileUploadShapes(\"inputFile-"+elementId+"\")'><i class='far fa-folder-open'></i></button>" +
                        "</div>" +
                        "<input type='text' id='inputFileName-"+elementId+"' class='form-control clickable' onclick='triggerFileUploadShapes(\"inputFile-"+elementId+"\")' readonly='readonly'/>" +
                    "</div>" +
                "</div>" +
                "<div class='col-md-8 col-sm-7 hidden' id='uriToValidate-"+elementId+"'>"+
                    "<input type='url' class='form-control' id='uri-"+elementId+"' name='uri-"+type+"'>"+
                "</div>"+
                "<input type='file' class='inputFile' id='inputFile-"+elementId+"' name='inputFile-"+type+"' onchange='fileInputChangedShapes(\""+elementId+"\")'/>" +
                "<div class='col-md-3 col-sm-3'>" +
                    "<select class='form-control' id='contentSyntaxType-"+elementId+"' name='contentSyntaxType-"+type+"'>" + options + "</select>" +
                "</div>" +
                "<div class='col-md-1 col-sm-2'>" +
                    "<button class='btn btn-default' type='button' onclick='removeElement(\""+elementId+"\")'><i class='far fa-trash-alt'></i></button>" +
                "</div>" +
    		"</div>"+
		"</div>"+
    "</div>").insertBefore("#"+type+"AddButton");
    $("#"+elementId+" input").focus();
}

function addExternalShapes(){
	addElement("externalShape");
}

function toggleExternalShapesClassCheck() {
    $(".externalShapesClass").toggle();
}

$(document).ready(function() {
	toggleExternalShapesClassCheck();

	var editableCodeMirror = CodeMirror.fromTextArea(document.getElementById('text-editor'), {
        mode: "xml",
        autoRefresh: false,
        gutter: true,
        lineWrapping: true,
        autoFormatOnStart: true,
        styleActiveLine: true,
        lineNumbers: true
    }).on('change', function(){
    	contentSyntaxChanged();
    });
    
});