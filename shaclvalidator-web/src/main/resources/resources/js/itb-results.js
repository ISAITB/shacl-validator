var resultReportID;

function getReportData(reportID) {
	resultReportID = reportID;
}
function downloadResult() {
	var type = $('#downloadType').val();
	var syntaxType = $('#downloadSyntaxType').val();
	var selectedIndex = document.getElementById("downloadSyntaxType").selectedIndex;
	var selectedSyntax = document.getElementById("downloadSyntaxType")[selectedIndex].text;
    $('#resultFormId').val(resultReportID);
    $('#resultFormType').val(type);
    $('#resultFormSyntax').val(syntaxType.replace("/", "_"));
    $('#resultForm').submit();
}

function downloadTypeChange(){
	var dType = $('#downloadType').val();
	var dSyntaxType = document.getElementById("downloadSyntaxType");
	
	dSyntaxType = removeOption(dSyntaxType, 'pdfType');
	dSyntaxType = removeOption(dSyntaxType, 'notSelect');
	
	if(dType == "reportType"){
		dSyntaxType.add(createOption("PDF", "pdfType"),0);
		dSyntaxType.selectedIndex = 0;
		
		dSyntaxType.add(createOption("-----------------------", "notSelect"),1);
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