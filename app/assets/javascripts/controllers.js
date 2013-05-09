'use strict';

/* 
 *	Search a repository (/search/:keyword).
 */
function RepositoryListCtrl($scope, $rootScope, $routeParams, $location, Github, Play) {
	var keyword = $routeParams.keyword  || '';
	$rootScope.keyword = keyword;
	if( keyword != ''){
		$('#loading').show();
        Github.search(keyword).then(function(response){
			$scope.repositories = response;
			$('#loading').hide();
		});
	}
    $scope.repoClick = function(owner, name){
        $location.path('/repo/'+ owner + '/' + name);
    }
	$scope.orderProp = 'name';
}

/* 
 *	Repository general informations (/repo/:owner/:repo).
 */
function RepositoryHomeCtrl($scope, $routeParams, $rootScope, Github, Play) {
	$('#loading').show();
	var owner = $routeParams.owner;
	var repo = $routeParams.repository;
    Github.repo(owner, repo).then(function(response){
		$scope.repository = response;
		$('#loading').hide();
	});
    if($rootScope.isConnected) {
        Play.indexRepo(owner, repo, $rootScope.token)
    }
    Play.getRepoReco(owner, repo).then(function(response){
        $scope.recos = response;
    })
    $('#sidenav').affix();
}

/* 
 *	Repository members informations (/repo/:owner/:repo/collaborators).
 */
function RepositoryMembersCtrl($scope, $routeParams, Github) {
	$('#loading').show();
	var owner = $routeParams.owner;
	var repo = $routeParams.repository;
	$scope.repository = {owner:{login:owner}, name:repo};
    Github.members(owner, repo).then(function(response){
		$scope.members = response;
	});
    Github.contributors(owner, repo).then(function(response){
		$scope.contributors = response;
		$('#loading').hide();
	});
	$('#sidenav').affix();
}

/* 
 *  Repository commits informations (/repo/:owner/:repo/commits).
 */
function RepositoryCommitsCtrl($scope, $routeParams, Github) {
	$('#loading').show();
	var owner = $routeParams.owner;
	var repo = $routeParams.repository;
	$scope.repository = {owner:{login:owner}, name:repo};
    Github.commits(owner, repo).then(function(response){
		$scope.commits = response;
		$('#loading').hide();
	});
	$('#sidenav').affix();
}

/* 
 *	Repository commits stats(/repo/:owner/:repo/stats/commit).
 */
function RepositoryStatsCommitCtrl($scope, $routeParams, Github) {
	$('#loading').show();
	var owner = $routeParams.owner;
	var repo = $routeParams.repository;
	$scope.repository = {owner:{login:owner}, name:repo};
	$scope.colors = ["#ff0000","#ff9900","#ccff00","#32ff00","#58E3AD","#00ffff","#4598FF","#2D00D4","#cb00ff","#ff0099"];
	$scope.labels = [];
	$scope.commits= [];
	$scope.additions= [];
	$scope.deletions= [];
    Github.commits(owner, repo).then(function(response){
		$scope.commitHistory = response;
		for (var i=0; i<$scope.commitHistory.length; i++) {
			if($scope.commitHistory[i].author && $scope.commitHistory[i].author.login){
				var index = $scope.labels.indexOf($scope.commitHistory[i].author.login);
				if(index == -1){
					$scope.labels.push($scope.commitHistory[i].author.login);
					$scope.commits.push(1);
					$scope.additions.push(0);
					$scope.deletions.push(0);
				}
				else{
					// commit stats
					var nbCommit = $scope.commits[index];
					nbCommit += 1;
					$scope.commits[index] = nbCommit;
				}
                Github.commit(owner, repo, $scope.commitHistory[i].sha).then(function(response){
					var loginIndex = $scope.labels.indexOf(response.author.login);
					// deletion stats
					var nbAddition = $scope.additions[loginIndex];
					nbAddition += response.stats.additions;
					$scope.additions[loginIndex] = nbAddition;
					// addition stats
					var nbDeletion = $scope.deletions[loginIndex];
					nbDeletion += response.stats.deletions;
					$scope.deletions[loginIndex] = nbDeletion;

					if($scope.commitHistory[$scope.commitHistory.length-1].sha == response.sha){
						$('#loading').hide();
					}
				});

			}
		}
	});
	$('#sidenav').affix();
}

/* 
 *  Repository geo stats (/repo/:owner/:repo/stats/geo).
 */
function RepositoryStatsGeoCtrl($scope, $routeParams, Github, Nominatim) {
	$('#loading').show();
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
    Github.contributors(owner, repo).then(function(response){
		// var to know when nominatim is done (hide spinner)
		$scope.nbContrib = response.length; 
		$scope.nbNominatim = 0;

		for (var i=0; i<$scope.nbContrib; i++) {
			if(response[i].login){
                Github.user(response[i].login).then(function(response){
					if(response.location){
						Nominatim.locate(response.location).then(function(locationResp){
							$scope.nbNominatim +=1;
							if(locationResp[0]){
								var markerLocation = new L.LatLng(locationResp[0].lat,locationResp[0].lon);
						        var marker = new L.Marker(markerLocation);
						        markers.addLayer(marker);
						    }
						    if($scope.nbNominatim == $scope.nbContrib){
						    	$('#loading').hide();
						    }
						})
					}
					else{
						// increment nbNomitatim anyway
						$scope.nbNominatim +=1;
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
    $('#loading').hide();
}