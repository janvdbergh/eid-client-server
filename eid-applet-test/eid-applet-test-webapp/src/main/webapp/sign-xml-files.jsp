<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<html>
<head>
<title>eID Applet XML Signature Test</title>
</head>
<body>
<h1>eID Applet XML Signature Test</h1>
<form method="post" action="sign-xml-files-applet.jsp">
<p>XML Signature Algorithm: <select name="signDigestAlgo">
	<option value="SHA-1" selected="selected">SHA-1 with RSA</option>
<!-- Support for other algos will be pushed in JSR105 via 6u16.
	<option value="SHA-256">SHA-256 with RSA</option>
	<option value="SHA-384">SHA-384 with RSA</option>
	<option value="SHA-512">SHA-512 with RSA</option>
	<option value="RIPEMD160">RIPEMD160 with RSA</option>
-->
</select></p>
<p>Digest Algorithm for the files: <select name="filesDigestAlgo">
	<option value="SHA-1" selected="selected">SHA-1</option>
	<option value="SHA-256">SHA-256</option>
	<option value="SHA-512">SHA-512</option>
</select></p>
<p><input type="submit" value="Continue" /></p>
</form>
</body>
</html>