'use strict';

/* 
 *	Search a repository (/search/:keyword).
 */
function RepositoryListCtrl($scope, $rootScope, $routeParams, $location, $github) {
	var keyword = $routeParams.keyword  || '';
	$scope.keyword = keyword;
	if( keyword != ''){
		$github.search(keyword).then(function(response){
			$scope.repositories = response;
		});
	}
	$scope.orderProp = 'name';
}

/* 
 *	Repository general informations (/repo/:owner/:repo).
 */
function RepositoryHomeCtrl($scope, $rootScope, $routeParams,$location, $github) {
	var owner = $routeParams.owner;
	var repo = $routeParams.repository;
	$github.repo(owner, repo).then(function(response){
		$scope.repository = response;
	});
    $('#sidenav').affix();
}

/* 
 *	Repository members informations (/repo/:owner/:repo/collaborators).
 */
function RepositoryMembersCtrl($scope, $rootScope, $routeParams, $location, $github) {
	var owner = $routeParams.owner;
	var repo = $routeParams.repository;
	$scope.repository = {owner:{login:owner}, name:repo};
	$github.members(owner, repo).then(function(response){
		$scope.members = response;
	});
	$github.contributors(owner, repo).then(function(response){
		$scope.contributors = response;
	});
	$('#sidenav').affix();
}

/* 
 *  Repository commits informations (/repo/:owner/:repo/commits).
 */
function RepositoryCommitsCtrl($scope, $rootScope, $routeParams,$location, $github) {
	var owner = $routeParams.owner;
	var repo = $routeParams.repository;
	$scope.repository = {owner:{login:owner}, name:repo};
	$github.commits(owner, repo).then(function(response){
		$scope.commits = response;
	});
	$('#sidenav').affix();
}

/* 
 *	Repository commits stats(/repo/:owner/:repo/stats/commit).
 */
function RepositoryStatsCommitCtrl($scope, $rootScope, $routeParams,$location, $github) {
	var owner = $routeParams.owner;
	var repo = $routeParams.repository;
	$scope.repository = {owner:{login:owner}, name:repo};
	$scope.colors = ["#ff0000","#ff9900","#ccff00","#32ff00","#00ff65","#00ffff","#0065ff","#3200ff","#cb00ff","#ff0099"];
	$scope.labels = [];
	$scope.commits= [];
	$scope.additions= [];
	$scope.deletions= [];
	$github.commits(owner, repo).then(function(response){
		var commitHistory = response;
		for (var i=0; i<commitHistory.length; i++) {
			if(commitHistory[i].author && commitHistory[i].author.login){
				if($scope.labels.indexOf(commitHistory[i].author.login) == -1){
					$scope.labels.push(commitHistory[i].author.login);
					var index = $scope.labels.indexOf(commitHistory[i].author.login);
					$scope.commits[index] = 0;
					$scope.additions[index] = 0;
					$scope.deletions[index] = 0;
				}
				$github.commit(owner, repo, commitHistory[i].sha).then(function(response){
					var loginIndex = $scope.labels.indexOf(response.author.login);
					// commit stats
					var nbCommit = $scope.commits[loginIndex];
					nbCommit += 1;
					$scope.commits[loginIndex] = nbCommit;
					// deletion stats
					var nbAddition = $scope.additions[loginIndex];
					nbAddition += response.stats.additions;
					$scope.additions[loginIndex] = nbAddition;
					// addition stats
					var nbDeletion = $scope.deletions[loginIndex];
					nbDeletion += response.stats.deletions;
					$scope.deletions[loginIndex] = nbDeletion;
				});

			}
		}
	});
	$('#sidenav').affix();
}

/* 
 *  Repository geo stats (/repo/:owner/:repo/stats/geo).
 */
function RepositoryStatsGeoCtrl($scope, $rootScope, $routeParams, $location, $github, $nominatim) {
	var owner = $routeParams.owner;
	var repo = $routeParams.repository;
	$scope.repository = {owner:{login:owner}, name:repo};
	// initialize the map on the "map" div
    var map = new L.Map('map');
    // create a CloudMade tile layer with style #997 (or use other provider of your choice)
    var cloudmade = new L.TileLayer('http://{s}.tile.cloudmade.com/BC9A493B41014CAABB98F0471D759707/997/256/{z}/{x}/{y}.png', {
        attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://cloudmade.com">CloudMade</a>',
        maxZoom: 17
    });
    // add the layer to the map, set the view to a given place and zoom
    map.addLayer(cloudmade).setView(new L.LatLng(0, 0), 1);
    // setup the clustering
    var markers = new L.MarkerClusterGroup();
    map.addLayer(markers);

    // Calling 
	$github.contributors(owner, repo).then(function(response){
		for (var i=0; i<response.length; i++) {
			if(response[i].login){
				$github.user(response[i].login).then(function(response){
					if(response.location){
						$nominatim.locate(response.location).then(function(locationResp){
							if(locationResp[0]){
								var markerLocation = new L.LatLng(locationResp[0].lat,locationResp[0].lon);
						        var marker = new L.Marker(markerLocation);
						        markers.addLayer(marker);
						    }
						})
					}
				})
			}
	    }
	});
	$('#sidenav').affix();
}

/*
 *  Error.
 */
function ErrorCtrl() {
}