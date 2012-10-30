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
	$scope.popupMember = function (member){
		$github.user(member.login).then(function(response){
			$scope.user = response;
			$('#member').modal('show');
		});
	};
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
function RepositoryStatsCommitCtrl($scope, $rootScope, $routeParams,$location, $play) {
	var owner = $routeParams.owner;
	var repo = $routeParams.repository;
	$scope.repository = {owner:{login:owner}, name:repo};
	$play.stats(owner, repo).then(function(response){
		var stats = response;
		var labels = [];
		var commits= [];
		var additions= [];
		var deletions= [];
		for (var i=0; i<stats.length; i++) {
			labels.push(stats[i].author.login);
			commits.push(stats[i].impacts.commits);
			additions.push(stats[i].impacts.additions);
			deletions.push(stats[i].impacts.deletions);
		}
		Raphael("commits", 700, 700).pieChart(350, 350, 200, commits, labels, "#fff");
			Raphael("additions", 700, 700).pieChart(350, 350, 200, additions, labels, "#fff");
			Raphael("deletions", 700, 700).pieChart(350, 350, 200, deletions, labels, "#fff");
		
	});
	$('#sidenav').affix();
}

/* 
 *  Repository geo stats (/repo/:owner/:repo/stats/geo).
 */
function RepositoryStatsGeoCtrl($scope, $rootScope, $routeParams,$location, $play) {
	$('#sidenav').affix();
}

/*
 *  Error.
 */
function ErrorCtrl() {
}