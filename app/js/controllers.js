'use strict';

/* 
	Search a repository (/search/:keyword).
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
	$rootScope.searchRepository = function() {
		var keyword = $scope.q;
		$location.path('/search/'+ keyword);
	}
}

/* 
	Repository general informations (/repo/:owner/:repo).
*/
function RepositoryHomeCtrl($scope, $rootScope, $routeParams,$location, $github) {
	var owner = $routeParams.owner;
	var repo = $routeParams.repository;
	$github.repo(owner, repo).then(function(response){
		$scope.repository = response;
	});
	$rootScope.searchRepository = function() {
		var keyword = $scope.q;
		$location.path('/search/'+ keyword);
	}
}

/* 
	Repository members informations (/repo/:owner/:repo/collaborators).
*/
function RepositoryMembersCtrl($scope, $rootScope, $routeParams, $location, $github) {
	var owner = $routeParams.owner;
	var repo = $routeParams.repository;
	$scope.repository = {owner:{login:owner}, name:repo};
	$github.members(owner, repo).then(function(response){
		$scope.members = response;
	});
	$scope.popupMember = function (member){
		$github.user(member.login).then(function(response){
			$scope.user = response;
			$('#member').modal('show');
		});
	};
	$rootScope.searchRepository = function() {
		var keyword = $scope.q;
		$location.path('/search/'+ keyword);
	}
}


/* 
	Repository commits informations (/repo/:owner/:repo/commits).
*/
function RepositoryCommitsCtrl($scope, $rootScope, $routeParams,$location, $play) {
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
	$rootScope.searchRepository = function() {
		var keyword = $scope.q;
		$location.path('/search/'+ keyword);
	}
}

function ErrorCtrl() {
}