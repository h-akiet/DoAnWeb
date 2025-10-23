<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="decorator" uri="http://www.opensymphony.com/sitemesh/decorator" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>OneShop</title>
</head>
<body>
    <div>
        <%@ include file="/common/header.jsp" %>
    </div>
    <decorator:body />
    <div>
        <%@ include file="/common/footer.jsp" %>
    </div>
</body>
</html>