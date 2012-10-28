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
	Repository commits informations (/repo/:owner/:repo/commits).
*/
function RepositoryCommitsCtrl($scope, $rootScope, $routeParams,$location, $github) {
	var owner = $routeParams.owner;
	var repo = $routeParams.repository;
	$scope.repository = {owner:{login:owner}, name:repo};
	$github.commits(owner, repo).then(function(response){
		$scope.commits = response;
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
	$rootScope.searchRepository = function() {
		var keyword = $scope.q;
		$location.path('/search/'+ keyword);
	}
}

function ErrorCtrl() {
}