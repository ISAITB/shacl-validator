function fileInputChanged() {
	$('#inputFileName').val($('#inputFile')[0].files[0].name);
	checkForSubmit();
}
function validationTypeChanged() {
	checkForSubmit();
}
function checkForSubmit() {
	var inputFile = $('#inputFileName');
	var inputType = $('#validationType');
	$('#inputFileSubmit').prop('disabled', (inputFile.val() && (!inputType.length || inputType.val()))?false:true);
}
function triggerFileUpload() {
	$('#inputFile').click();
}
function uploadFile() {
	waitingDialog.show('Validating input', {dialogSize: 'm'});
	return true;
}
function removeElement(elementId) {
    $("#"+elementId).remove();
}

function addElement(type) {
    var elements = $("."+type+"Div").length;
    var elementId = type+"-"+elements;
    $("<div class='input-group "+type+"Div col-sm-4' id='"+elementId+"'>" +
    		"<input type='text' name="+type+" class='form-control'/>" +
    		"<div class='input-group-btn'>" +
    			"<button class='btn btn-default' type='button' onclick='removeElement(\""+elementId+"\")'><i class='far fa-trash-alt'></i></button>" +
    		"</div></div>").insertBefore("#"+type+"AddButton");
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
});