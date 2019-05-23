var itbReportData;
var itbResultReportXML;
var itbResultReportPDF;
var reportLoad = jQuery.Deferred();
var resultLoadXML = jQuery.Deferred();
var resultLoadPDF = jQuery.Deferred();
function getReportData(xmlID) {
	getReport(xmlID);
	getResultReport(xmlID);
}
function getReport(xmlID) {
	$.get("xml/"+xmlID, function(data) {
		itbReportData = data;
		$.ajax({
			url: "xml/"+xmlID,
			type: 'DELETE'
		});
		reportLoad.resolve();
		$('#viewInputButton').prop('disabled', false);
	});
}
function getResultReport(xmlID) {
	$.ajax({
		url: "report/"+xmlID+"/xml",
		type: 'GET',
		success: function(data) {
			itbResultReportXML = new Blob([data], { type: 'application/xml' });
            $('#downloadReportButtonXML').prop('disabled', false);
			resultLoadXML.resolve();
		}
	});

    var ajax = new XMLHttpRequest();
    ajax.open("GET", "report/"+xmlID+"/pdf", true);
    ajax.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status == 200) {
                itbResultReportPDF = new Blob([this.response], {type: "application/octet-stream"});
                $('#downloadReportButtonPDF').prop('disabled', false);
                resultLoadPDF.resolve();
            }
        } else if (this.readyState == 2) {
            if (this.status == 200) {
                this.responseType = "blob";
            } else {
                this.responseType = "text";
            }
        }
    };
    ajax.send(null);
	$.when(resultLoadXML, resultLoadPDF).done(function () {
        $.ajax({
            url: "report/"+xmlID,
            type: 'DELETE'
        });
	})
}
function downloadReportXML() {
	resultLoadXML.done(function() {
		saveAs(itbResultReportXML, "report.xml");
	});
}
function downloadReportPDF() {
	resultLoadPDF.done(function() {
		saveAs(itbResultReportPDF, "report.pdf");
	});
}
function getLineFromPositionString(positionString) {
	var line = parseInt(positionString.trim().split(':')[1])-1;
	if (line < 0) {
		line = 0;
	}
	return line;
}
function setCode(reportItemElement) {
	reportLoad.done(function() {
		var editorContent = $('#xml-content-pane');
		editorContent.empty();
		var cm = CodeMirror(editorContent[0], {
			value: itbReportData,
			mode:  "xml",
			lineNumbers: true,
			readOnly: true,
			dragDrop: false
		});
		// Add report messages
		$('.item-info').each(function(index, element) {
			var line = getLineFromPositionString($(this).find('.item-info-location').text());
			var text = $(this).find('.item-info-text').text().trim();
			var type = 'info';
			var indicatorIcon = '<i class="fa fa-info-circle report-item-icon info-icon"></i>';
			if ($(this).find('.item-info-error').length) {
				type = 'error';
				indicatorIcon = '<i class="fa fa-times-circle report-item-icon error-icon"></i>';
			} else if ($(this).find('.item-info-warning').length) {
				type = 'warning';
				indicatorIcon = '<i class="fa fa-exclamation-triangle report-item-icon warning-icon"></i>';
			}
			var indicator = $('<div class="indicator-editor-widget indicator-'+type+'">' +
					'<span class="indicator-icon">' +
						indicatorIcon +
					'</span>'+
					'<span class="indicator-desc">' +
						text +
					'</span>' +
				'</div>');
			cm.addLineWidget(line, indicator[0], {
				coverGutter: false,
				noHScroll: true,
				above: true
			});
			cm.getDoc().addLineClass(line, 'background', 'indicator-line-widget');
		});
		$('#xml-content-modal').modal('show');
		$('#xml-content-modal').on('shown.bs.modal', function() {
			cm.refresh();
			if (reportItemElement) {
				var line = getLineFromPositionString($(reportItemElement).find('.item-info-location').text());
				cm.getDoc().addLineClass(line, 'background', 'selected-editor-line');
				cm.markText({line: line, ch: 0}, {line: line+1, ch: 0}, {className: 'selected-editor-line-text'});
				var t = cm.charCoords({line: line, ch: 0}, "local").top;
				var middleHeight = cm.getScrollerElement().offsetHeight / 2;
				cm.scrollTo(null, t - middleHeight - 5);
			}
		});
	});
}
