<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>Crashboard</title>


    <!-- Bootstrap core CSS -->
    <!-- Latest compiled and minified CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css">
    <link rel="stylesheet" href="/css/crashboard.css">

    <script src="/Chart.min.js"></script>
</head>
<body>

<nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
    <div class="container-fluid">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false" aria-controls="navbar">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">CrashFX Dashboard</a>
        </div>
        <div id="navbar" class="navbar-collapse collapse">
            <ul class="nav navbar-nav navbar-right">
                <!--<li><a href="#">Dashboard</a></li>
                <li><a href="#">Settings</a></li>
                <li><a href="#">Profile</a></li>
                <li><a href="#">Help</a></li>-->
            </ul>
            <form class="navbar-form navbar-right">
                <input type="text" class="form-control" placeholder="Search...">
            </form>
        </div>
    </div>
</nav>

<div class="container-fluid">
    <div class="row">
        <div class="col-sm-3 col-md-2 sidebar">
            <ul class="nav nav-sidebar">
                <li class="active"><a href="#">Overview <span class="sr-only">(current)</span></a></li>
            </ul>
        </div>
        <div class="col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main">
            <h1 class="page-header">Dashboard</h1>

            <div class="row">
                <div class="col-md-4">
                    <canvas id="crashChart" width="300" height="300"></canvas>
                </div>
                <div class="col-md-4">
                    Top crashers in the last 100 exceptions:

                    <ul class="list-group">
                        <#list tops as t>
                        <li class="list-group-item">
                            <span class="badge">${t.count}</span>
                            ${t.type}
                        </li>
                        </#list>
                    </ul>

                </div>
            </div>


            <script>
                var ctx = document.getElementById("crashChart").getContext("2d");
                var data = [
                    <#list tops as t>
                    {
                        value: ${t.count},
                        label: "${t.type}",
                        color:"${t.color}",
                        highlight: "${t.colorHighlight}"
                    },
                    </#list>
                ];
                var pie = new Chart(ctx).Doughnut(data, {
                    animateScale: true
                });

                function toggle(id) {
                    var el = document.getElementById(id);
                    if (el.style.display == "none") {
                        el.style.display = ""
                    } else {
                        el.style.display = "none"
                    }
                }
            </script>
            <div class="table-responsive">
                <table class="table table-striped">
                    <thead>
                    <tr>
                        <th>#</th>
                        <th>Timestamp</th>
                        <th>Type</th>
                        <th>AppID</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list crashes as c>
                    <tr class="crash-row" onclick="toggle('log-for-${c.id}')">
                        <td>${c.id}</td>
                        <td>${c.timestamp}</td>
                        <td>${c.exceptionTypeName!"Unknown"}</td>
                        <td>${c.appID!"Unknown"}</td>
                    </tr>
                    <tr style="display: none" id="log-for-${c.id}">
                        <td colspan="4">
                            <pre>${c.log}</pre>
                        </td>
                    </tr>
                    </#list>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>


<!-- Bootstrap core JavaScript
================================================== -->
<!-- Placed at the end of the document so the pages load faster -->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
<!-- Latest compiled and minified JavaScript -->
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script></head>
</body>
</html>
