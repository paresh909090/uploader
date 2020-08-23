<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
<title>TurkLex</title>
<!-- Bootstrap -->

<link href="<c:url value="/resources/css/bootstrap.css" />"
	rel="stylesheet">
<link href="<c:url value="/resources/css/core.css" />" rel="stylesheet">
<link href="<c:url value="/resources/css/font-awesome.css" />"
	rel="stylesheet">
<!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
<!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
<!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js"></script>
      <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->

<script type="text/javascript">
	var getUrlParameter = function getUrlParameter(sParam) {
		var sPageURL = window.location.search.substring(1), sURLVariables = sPageURL
				.split('&'), sParameterName, i;

		for (i = 0; i < sURLVariables.length; i++) {
			sParameterName = sURLVariables[i].split('=');

			if (sParameterName[0] === sParam) {
				return sParameterName[1] === undefined ? true
						: decodeURIComponent(sParameterName[1]);
			}
		}
	};

	function SaveAsFile(t,f,m) {
        try {
            var b = new Blob([t],{type:m});
            saveAs(b, f);
        } catch (e) {
            console.log(e);
            window.open("data:"+m+"," + encodeURIComponent(t), '_blank','');
        }
    }

	function getResources() {
		var p = getUrlParameter("p");
		var api_url = "http://localhost:8080/resource";
		if (typeof p != "undefined") {
			api_url = api_url + "?p=" + p;
		}
		
		$.ajax({
			url : api_url,
			headers : {
				'Access-Control-Allow-Origin' : '*',
				'Content-Type' : 'application/json'
			},
			success : function(data) {
				generateResourceListing(data);
				console.log(data);
			},
			error : function(error) {
				console.log(error);
			}
		})
	}

	function getBreadcrumb() {
		var p = getUrlParameter("p");
		var api_url = "http://localhost:8080/resource/breadcrumb";
		if (typeof p != "undefined") {
			api_url = api_url + "?p=" + p;
		}
		$.ajax({
			url : api_url,
			headers : {
				'Access-Control-Allow-Origin' : '*',
				'Content-Type' : 'application/json'
			},
			success : function(data) {
				data.reverse();
				$.each(
						data,
						function(idx, resource) {
							increaseBreadcrumbLevel(resource.resourceId, resource.resourceOriginalName);
						});
			},
			error : function(error) {
				console.log(error);
			}
		})
	}

	function humanFileSize(size) {
		if(size === 0 || size == 'undefined') {
			return 0;
		}
	    var i = Math.floor( Math.log(size) / Math.log(1000) );
	    return ( size / Math.pow(1000, i) ).toFixed(1) * 1 + ' ' + ['B', 'kB', 'MB', 'GB', 'TB'][i];
	};

	function generateResourceListing(resources) {
		var tableBody = "";

		$.each(
			resources,
			function(idx, resource) {
				if (resource.resourceType === "FOLDER") {
					compiledRow = "<tr><td data-title='Select Document' class='checkDesign'><input name='selected[]' type='checkbox' data-type='FOLDER' data-id='"+resource.resourceId+"' id='"+resource.resourceId+"'> <label for='"+resource.resourceId+"'></label></td><td>"
							+ resource.resourceType
							+ "</td><td class='hand-cursor resource folder' data-name='"+resource.resourceOriginalName+"' data-type='FOLDER' data-i='"+resource.resourceId+"'>"
							+ resource.resourceOriginalName
							+ "</td><td>"+ humanFileSize(resource.sizeInBytes) +"</td><td>"+ moment(resource.createDateTime).format("DD MMM YYYY hh:mm a") +"</td><td>"+ moment(resource.lastAccessedDateTime).format("DD MMM YYYY hh:mm a") +"</td></tr>";
				} else if (resource.resourceType === "FILE") {
					compiledRow = "<tr><td data-title='Select Document' class='checkDesign'><input name='selected[]' type='checkbox' data-type='FILE' data-id='"+resource.resourceId+"' id='"+resource.resourceId+"'> <label for='"+resource.resourceId+"'></label></td><td>"
							+ resource.resourceType
							+ "</td><td class='hand-cursor resource file' data-name='"+resource.resourceOriginalName+"' data-type='FILE' data-i='"+resource.resourceId+"'>"
							+ resource.resourceOriginalName
							+ "</td><td>"+ humanFileSize(resource.sizeInBytes)+"</td><td>"+  moment(resource.createDateTime).format("DD MMM YYYY hh:mm a")  +"</td><td>"+ moment(resource.lastAccessedDateTime).format("DD MMM YYYY hh:mm a") +"</td></tr>";
				}

				tableBody = tableBody + compiledRow;
			});

		$("#resourceslisting tbody").empty().append(tableBody);
	}

	function updateResourceListingAndIncreaseBreadcrumbLevel(resourceId, folderName) {
		getResources();
		increaseBreadcrumbLevel(resourceId, folderName);
	}

	function increaseBreadcrumbLevel(resourceId, folderName) {

		var compiledBreadcrumb = "<li class='breadcrumb-item' data-i='"+resourceId+"' data-name='"+folderName+"'><a href='javascript:void(0)'>"+folderName+"</a></li>";
		$("#breadcrumb").append(compiledBreadcrumb);		
	}

	function updateResourceListingAndDecreaseBreadcrumbLevel(resourceId) {
		getResources();
		decreaseBreadcrumbLevel(resourceId);
	}

	function decreaseBreadcrumbLevel(resourceId) {
		
		$('.breadcrumb-item').filter("[data-i='"+resourceId+"']").nextAll().remove();
	}
 
	function insertUrlParam(key, value) {
	    if (history.pushState) {
	        let searchParams = new URLSearchParams(window.location.search);
	        searchParams.set(key, value);
	        let newurl = window.location.protocol + "//" + window.location.host + window.location.pathname + '?' + searchParams.toString();
	        window.history.pushState({path: newurl}, '', newurl);
	    }
	}

	function generateBreadcrumb(folderId) {
		
	}
 
	// Wait for the page to load first
	window.onload = function() {

		getResources();
		getBreadcrumb();

		$("#fileUploadBtn").on('click', function(){
			var data = new FormData();
		    data.append('file', $('#fileUpload')[0].files[0]);
		    data.append('resourceType', "FILE");
		    var p = getUrlParameter("p");
			if (typeof p != "undefined") {
				data.append('parentResourceId', p);	
			}

			$.ajax({
		        url: 'http://localhost:8080/resource/add',
		        data: data,
		        processData: false,
		        type: 'POST',
		        contentType: false,
		        success: function (data) {
		        	$('#fileUpload').val('');
		            $("#uploadResourceModal").modal('hide');
		            getResources();
		        }
		    });
		});

		$("#zipDownloadBtn").on('click', function(){
			$("input:checkbox[name='selected[]']:checked").each(function() {
			    console.log($(this).data("type"));
			    if($(this).data("type") === 'FOLDER') {
			    	var data = new FormData();
			    	data.append('resourceType', "FOLDER");
			    	data.append('resourceId', $(this).data("id"));
				    var p = getUrlParameter("p");
					if (typeof p != "undefined") {
						data.append('parentResourceId', p);	
					}

					$.ajax({
				        url: 'http://localhost:8080/resource/download',
				        data: data,
				        headers : {
							'Access-Control-Allow-Origin' : '*'
						},
				        processData: false,
				        type: 'POST',
				        contentType: false,
				        xhrFields:{
			                responseType: 'arraybuffer'
			            },
				        success: function (data, status, xhr) {
				        	var fileName = xhr.getResponseHeader('Content-Disposition').split("filename=")[1];
					        SaveAsFile(data,fileName,xhr.getResponseHeader('content-type'));
				        },
				        error : function(data) {
							console.log(data);
						}
				    });
				}else if($(this).data("type") === 'FILE') {
					var data = new FormData();
					data.append('resourceId', $(this).data("id"));
					data.append('resourceType', "FILE");

					$.ajax({
				        url: 'http://localhost:8080/resource/download',
				        data: data,
				        headers : {
							'Access-Control-Allow-Origin' : '*'
						},
				        processData: false,
				        type: 'POST',
				        contentType: false,
				        xhrFields:{
			                responseType: 'arraybuffer'
			            },
				        success: function (data, status, xhr) {
				        	var fileName = xhr.getResponseHeader('Content-Disposition').split("filename=")[1];
				        	SaveAsFile(data,fileName,xhr.getResponseHeader('content-type'));
				        },
				        error : function(data) {
							console.log(data);
						}
				    });
				}
			});
		});

		$("#zipImportBtn").on('click', function(){
			var data = new FormData();
		    data.append('file', $('#zipImport')[0].files[0]);
		    data.append('resourceType', "FILE");
		    var p = getUrlParameter("p");
			if (typeof p != "undefined") {
				data.append('parentResourceId', p);	
			}

			$.ajax({
		        url: 'http://localhost:8080/resource/import',
		        data: data,
		        processData: false,
		        type: 'POST',
		        contentType: false,
		        success: function (data) {
		        	$('#zipImport').val('');
		            $("#importZipFileModal").modal('hide');
		            getResources();
		        }
		    });
		});

		$("#deleteBtn").on('click', function(){
			var apiURL = "http://localhost:8080/resource/delete";
			var param = "";

			$("input:checkbox[name='selected[]']:checked").each(function() {
			    if($(this).data("id")) {
					param = param + "p="+ $(this).data("id") + "&";
			    }
			 });
				
			$.ajax({
		        url: apiURL + "?" + param,
		        processData: false,
		        contentType: false,
		        success: function (data) {
			        $("#confirmDeleteModal").modal('hide');
		            getResources();
		        }
		    });
		});

		$("#saveFolder").on('click', function() {

			var data = new FormData();
			data.append('resourceOriginalName', $("#newfoldername").val());
			data.append('resourceType', "FOLDER");

			var p = getUrlParameter("p");
			if (typeof p != "undefined") {
				data.append('parentResourceId', p);	
			}
			
			$.ajax({
		        url: 'http://localhost:8080/resource/add',
		        data: data,
		        processData: false,
		        type: 'POST',
		        contentType: false,
		        success: function (data) {
		        	$("#newfoldername").val("");
		            $("#createFolderModal").modal('hide');
		            getResources();
		        }
		    });
		});

		$('#resourceslisting tbody').on('dblclick', '.resource', function() {
			if($(this).attr("data-type")==="FOLDER") {
				insertUrlParam("p", $(this).attr("data-i"));
				updateResourceListingAndIncreaseBreadcrumbLevel($(this).attr("data-i"), $(this).attr("data-name"));	
			}else if($(this).attr("data-type")==="FILE") {
				var win = window.open("http://localhost:8080/resource/view/" + $(this).attr("data-i"),
				'_blank');
			}
		});

		$('#breadcrumb').on('click', '.breadcrumb-item', function() {
			var p = $(this).attr("data-i");
			if(p === "0") {
				insertUrlParam("p", "");
				updateResourceListingAndDecreaseBreadcrumbLevel($(this).attr("data-i"));
			}else {
				insertUrlParam("p", p);
				updateResourceListingAndDecreaseBreadcrumbLevel($(this).attr("data-i"));
			}			
		});
	}
</script>

<style type="text/css">
.hand-cursor {
	cursor: pointer;
}
</style>

</head>
<body>
	<header class="defineFloat" id="header">
		<div class="container">
			<div class="row">
				<nav class="navbar navbar-default">
					<!-- Brand and toggle get grouped for better mobile display -->

					<!-- Collect the nav links, forms, and other content for toggling -->
					<div class="collapse navbar-collapse col-sm-9 col-xs-12 menuOuter"
						id="bs-example-navbar-collapse-1">
						<form class="navbar-form navbar-left">
							<div class="form-group">
								<input type="text" class="form-control"
									placeholder="Search documents...">
							</div>
						</form>

					</div>
					<!-- /.navbar-collapse -->

				</nav>
			</div>
		</div>
	</header>
	<section class="defineFloat" id="documents">
		<div class="container pad-0">
			<div class="row">
				<div class="documentLeft">
					<span class="show-icon"><i class="fa fa-angle-right"
						aria-hidden="true"></i></span>
					<div class="defineFloat documentList">
						<div class="defineFloat documentSearch">
							<form>
								<div class="form-group">
									<input type="text" class="form-control"
										placeholder="Search actions...">
								</div>
							</form>
						</div>
						<ul class="list-unstyled">
							<li><a href="javascript:void(0);" data-toggle="modal" data-target="#createFolderModal"><img
									src="<c:url value="/resources/svg/d1.svg" />"
									class="img-responsive" alt="" />Create Folder</a></li>
							<li><a href="javascript:void(0);" data-toggle="modal" data-target="#importZipFileModal"><img
									src="<c:url value="/resources/svg/d8.svg" />"
									class="img-responsive" alt="" />Import Zip</a></li>
							<li><a href="javascript:void(0);" data-toggle="modal" data-target="#uploadResourceModal"><img
									src="<c:url value="/resources/svg/d2.svg" />"
									class="img-responsive" alt="" />Upload</a></li>
							<li><a id="zipDownloadBtn" href="javascript:void(0);"><img
									src="<c:url value="/resources/svg/d3.svg" />"
									class="img-responsive" alt="" />Download</a></li>
							<%-- <li><a id="view" href="javascript:void(0);"><img
									src="<c:url value="/resources/svg/d5.svg" />"
									class="img-responsive" alt="" />View Html</a></li> --%>
							<li><a id="deleteBtn1" href="javascript:void(0);" data-toggle="modal" data-target="#confirmDeleteModal"><img
									src="<c:url value="/resources/svg/d4.svg" />"
									class="img-responsive" alt="" />Delete</a></li>
						</ul>
					</div>
				</div>
				<div class="documentRight" id="tableResponsive">

					<nav aria-label="breadcrumb">
						<ol class="breadcrumb" id="breadcrumb">
							<li class="breadcrumb-item" data-i="0" data-name="Home"><a href="javascript:void(0)">Home</a></li>
						</ol>
					</nav>

					<table class="table" id="resourceslisting">
						<colgroup>
							<col span="1" style="width: 10%;">
							<col span="1" style="width: 10%;">
							<col span="1" style="width: 30%;">
							<col span="1" style="width: 10%;">
							<col span="1" style="width: 20%;">
							<col span="1" style="width: 20%;">
						</colgroup>
						<thead>
							<tr>
								<th>Select</th>
								<th>Type</th>
								<th>Name</th>
								<th>Size</th>
								<th>Upload Date</th>
								<th>Last Modified Date</th>
							</tr>
						</thead>
						<tbody>

						</tbody>
					</table>
				</div>
			</div>
		</div>
	</section>
	
	<!-- Modals Start -->
	<div class="modal fade" id="createFolderModal" tabindex="-1" role="dialog" aria-labelledby="exampleModalCenterTitle" aria-hidden="true">
	  <div class="modal-dialog modal-dialog-centered" role="document">
	    <div class="modal-content">
	      <div class="modal-header">
	        <h5 class="modal-title" id="exampleModalLongTitle">Create Folder</h5>
	        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
	          <span aria-hidden="true">&times;</span>
	        </button>
	      </div>
	      <div class="modal-body">
	        <input type="text" class="form-control" id="newfoldername">
	      </div>
	      <div class="modal-footer">
	        <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
	        <button type="button" id="saveFolder" class="btn btn-primary">Create Folder</button>
	      </div>
	    </div>
	  </div>
	</div>
	
	
	<!-- Modal -->
	<div id="uploadResourceModal" class="modal fade" role="dialog">
	  <div class="modal-dialog">
	
	    <!-- Modal content-->
	    <div class="modal-content">
	      <div class="modal-header">
	        <button type="button" class="close" data-dismiss="modal">&times;</button>
	        <h4 class="modal-title">Upload file</h4>
	      </div>
	      <div class="modal-body">
	        <!-- Form -->
	        <form>
	          Select file : <input type='file' name='file' id='fileUpload' class='form-control' ><br>
	          <input type='button' class='btn btn-info' value='Upload' id='fileUploadBtn'>
	        </form>
	
	        <!-- Preview-->
	        <div id='preview'></div>
	      </div>
	 
	    </div>
	
	  </div>
	</div>
	
	<!-- Modal -->
	<div id="importZipFileModal" class="modal fade" role="dialog">
	  <div class="modal-dialog">
	
	    <!-- Modal content-->
	    <div class="modal-content">
	      <div class="modal-header">
	        <button type="button" class="close" data-dismiss="modal">&times;</button>
	        <h4 class="modal-title">Import Zip</h4>
	      </div>
	      <div class="modal-body">
	        <!-- Form -->
	        <form>
	          Select file : <input type='file' name='file' id='zipImport' class='form-control' accept='.zip,.rar,.7zip' ><br>
	          <input type='button' class='btn btn-info' value='Upload' id='zipImportBtn'>
	        </form>
	
	        <!-- Preview-->
	        <div id='preview'></div>
	      </div>
	 
	    </div>
	
	  </div>
	</div>
	
	<div class="modal" id="confirmDeleteModal" style="display: none; z-index: 1050;">
	    <div class="modal-dialog">
	        <div class="modal-content">
	            <div class="modal-body" id="confirmMessage">
	            	Are you sure you want to delete?
	            </div>
	            <div class="modal-footer">
	                <button type="button" class="btn btn-default" id="deleteBtn"  data-dismiss="modal">Delete</button>
	                <button type="button" class="btn btn-default" id="confirmDeleteCancel" data-dismiss="modal">Cancel</button>
	            </div>
	        </div>
	    </div>
	</div>
	<!-- Modals End -->
	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
	<script
		src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script>
	<!-- Include all compiled plugins (below), or include individual files as needed -->
	<script src="<c:url value="/resources/js/bootstrap.min.js" />"></script>
	<script src="<c:url value="/resources/js/core.js" />"></script>
	<script src="<c:url value="/resources/js/filesaver.js" />"></script>
	<script src="<c:url value="/resources/js/moment.js" />"></script>
</body>
</html>