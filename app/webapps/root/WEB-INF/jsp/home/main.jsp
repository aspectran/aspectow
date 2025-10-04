<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<style>
    .card {
        transition: transform 0.2s ease-in-out;
    }
    .card:hover {
        transform: translateY(-5px);
    }
    .card-header a {
        color: inherit;
        text-decoration: none;
    }
    .card img {
        border-bottom-left-radius: 0;
        border-bottom-right-radius: 0;
    }
</style>
<h1 class="display-5 mt-5">Aspectow Sample Applications</h1>
<p class="lead"><strong>Aspectow</strong> is an <strong>enterprise WAS</strong> product, optimized and stabilized for specific purposes based on the powerful open-source project <strong>Aspectran</strong>.
    It particularly focuses on solving problems that developers face in Microservice Architecture (MSA) environments.</p>
<p>The sample applications below feature a practical enterprise stack composed of industry-leading libraries. Check out the powerful features and stability of Aspectow for yourself through each demo.</p>
<h2 class="mt-5">Demo List</h2>
<div class="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-4 pt-4">
    <div class="col">
        <div class="card h-100 shadow-sm">
            <a class="d-none d-md-block" href="https://jpetstore.aspectran.com/"
               title="JPetStore is a full-stack sample web application built on top of MyBatis 3, Aspectran 9."><img src="https://assets.aspectran.com/img/demo/jpetstore.png" class="img-fluid"/></a>
            <div class="card-header">
                <a href="<aspectran:profile expression="prod">https://jpetstore.aspectran.com/</aspectran:profile><aspectran:profile expression="!prod"><aspectran:url value="/jpetstore/"/></aspectran:profile>">JPetStore Demo</a>
            </div>
            <div class="card-body">
                <p class="card-text">The goal of the JPetStore Demo App is to provide and demonstrate a sample web application that leverages Aspectran and MyBatis. </p>
            </div>
        </div>
    </div>
    <div class="col">
        <div class="card h-100 shadow-sm">
            <a class="d-none d-md-block" href="https://petclinic.aspectran.com/"
               title="PetClinic is a full-stack sample web application built on top Aspectran 9."><img src="https://assets.aspectran.com/img/demo/petclinic.png" class="img-fluid"/></a>
            <div class="card-header">
                <a href="<aspectran:profile expression="prod">https://petclinic.aspectran.com/</aspectran:profile><aspectran:profile expression="!prod"><aspectran:url value="/petclinic/"/></aspectran:profile>">PetClinic Demo</a>
            </div>
            <div class="card-body">
                <p class="card-text">The goal of the PetClinic Demo App is to provide and demonstrate a sample web application that leverages Aspectran and JPA. </p>
            </div>
        </div>
    </div>
    <div class="col">
        <div class="card h-100 shadow-sm">
            <a class="d-none d-md-block" href="https://demo.aspectran.com/"
               title="Aspectran Examples"><img src="https://assets.aspectran.com/img/demo/demo.png" class="img-fluid"/></a>
            <div class="card-header">
                <a href="<aspectran:profile expression="prod">https://demo.aspectran.com/</aspectran:profile><aspectran:profile expression="!prod"><aspectran:url value="/demo/"/></aspectran:profile>">Aspectran Examples</a>
            </div>
            <div class="card-body">
                <p class="card-text">The goal of the PetClinic Demo App is to provide and demonstrate a sample web application that leverages Aspectran and JPA.</p>
            </div>
        </div>
    </div>
    <div class="col">
        <div class="card h-100 shadow-sm">
            <a class="d-none d-md-block" href="<aspectran:url value="/monitoring/"/>" title="View logs with AppMon"><img src="https://assets.aspectran.com/img/demo/appmon.png" class="img-fluid"/></a>
            <div class="card-header">
                <a href="<aspectran:url value="/monitoring/"/>">Aspectow AppMon</a>
            </div>
            <div class="card-body">
                <p class="card-text">Aspectow AppMon is a lightweight, real-time monitoring solution for applications based on the Aspectran framework.</p>
            </div>
        </div>
    </div>
</div>