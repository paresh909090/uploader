
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>File Upload</title>
<script src="<%=request.getContextPath()%>/js/jquery-3.1.1.js"
	type="text/javascript"></script>
<script
	src="<%=request.getContextPath()%>/js/bootstrap-3.3.7-dist/js/bootstrap.min.js"></script>
<script
	src="<%=request.getContextPath()%>/js/dropzone-4.3.0/dist/dropzone.js"></script>


<link rel="stylesheet"
	href="<%=request.getContextPath()%>/js/bootstrap-3.3.7-dist/css/bootstrap.min.css">
<link rel="stylesheet"
	href="<%=request.getContextPath()%>/js/bootstrap-3.3.7-dist/css/bootstrap-theme.min.css">
<link rel="stylesheet"
	href="<%=request.getContextPath()%>/js/dropzone-4.3.0/dist/dropzone.css">

<style>


div.ex {
  max-width:500px;
  margin: auto;
  border: 3px solid #73AD21;
  text-align: center;
}

div.bt {
  max-width:500px;
  margin: auto;
  text-align: center;
}

</style>
</head>
<body>
</head>
<body>
<div class="ex">
    <p>Save file to database</p>
    <form enctype="multipart/form-data" action="uploadServlet" method="POST" class="dropzone dz-clickable" id="fileUploadForm">
        <div class="dz-message">Drop files here or click to upload.</div>

        <div class="bt">
            <input name="submit" type="submit" value="Save">
        </div>

    </form>
</div>

</body>
</html>