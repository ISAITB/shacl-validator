function fileInputChanged() {
	$('#inputFileName').val($('#inputFile')[0].files[0].name);
	checkForSubmit();
}
function fileInputChangedShapes(type){
	$("#inputFileName-"+type+"").val($("#inputFile-"+type+"")[0].files[0].name);
}
function validationTypeChanged() {
	checkForSubmit();
}
function checkForSubmit() {
	var inputFile = $("#inputFileName");
	var inputType = $('#validationType');
	$('#inputFileSubmit').prop('disabled', (inputFile.val() && (!inputType.length || inputType.val()))?false:true);
}
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

function addElement(type) {
    var elements = $("."+type+"Div").length;
    var elementId = type+"-"+elements;
    var options = $( "#contentSyntaxType" ).html();
    
    $("<ul class='input-group "+type+"Div row-sm item' id='"+elementId+"'>" +
    		"<div class='col-sm-6'>" +
    	    	"<div class='input-group' id='fileToValidate"+ type +"'>" +
    	    		"<div class='input-group-btn'>" +
    	    			"<button class='btn btn-default' type='button' onclick='triggerFileUploadShapes(\"inputFile-"+elementId+"\")'><i class='fa fa-folder-open'></i></button>" +
    	    		"</div>" +
    	    		"<input type='text' id='inputFileName-"+elementId+"' class='form-control' onclick='triggerFileUploadShapes(\"inputFile-"+elementId+"\")' readonly='readonly'/>" +
    	    	"</div>"+
	    		"<input type='file' class='inputFile' id='inputFile-"+elementId+"' name='inputFile-"+type+"' onchange='fileInputChangedShapes(\""+elementId+"\")'/>" +
	    	"</div>" +
	    	"<div class='col-sm-6'>" +
				"<select class='form-control' id='contentSyntaxType-"+elementId+"' name='contentSyntaxType-"+type+"'>" + options + "</select>" +
		    "</div>" +
    		"<div class='input-group-btn'>" +
    			"<button class='btn btn-default' type='button' onclick='removeElement(\""+elementId+"\")'>" +
    					"<i class='far fa-trash-alt'></i>" +
    			"</button>" +
    		"</div>" +
    "</ul>").insertBefore("#"+type+"AddButton");

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