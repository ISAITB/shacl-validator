var resultReportID;
var inputContentID;

function getReportData(reportID, inputContentId) {
	resultReportID = reportID;
	inputContentID = inputContentId;
}
function downloadResult() {
	var type = $('#downloadType').val();
	var syntaxType = $('#downloadSyntaxType').val();
	var selectedIndex = document.getElementById("downloadSyntaxType").selectedIndex;
	var selectedSyntax = document.getElementById("downloadSyntaxType")[selectedIndex].text;
    $('#resultFormId').val(resultReportID);
    $('#resultFormContentId').val(inputContentID);
    $('#resultFormType').val(type);
    $('#resultFormSyntax').val(syntaxType.replace("/", "_"));
    $('#resultForm').submit();
}
