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

function addExternalRules(){
	$.ajax({
		url: "/"+domain+"/upload",
		type: 'POST',
		name: 'addRow',
		success: function(data) {
			 $('.externalRules_list').html(data);
		}
	});
}