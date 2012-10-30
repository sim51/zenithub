'use strict';

/* App Module */
var app = angular.module('zenithub', ['github']);

app.config(function($routeProvider) {
		$routeProvider
			.when('/error', {templateUrl: 'partials/error.html', controller: ErrorCtrl})
	      	.when('/repo/:owner/:repository', {templateUrl: 'partials/repository.html', controller: RepositoryHomeCtrl})
	      	.when('/repo/:owner/:repository/commits', {templateUrl: 'partials/commits.html', controller: RepositoryCommitsCtrl})
	      	.when('/repo/:owner/:repository/members', {templateUrl: 'partials/members.html', controller: RepositoryMembersCtrl})
	      	.when('/search/:keyword', {templateUrl: 'partials/search.html',   controller: RepositoryListCtrl})
	      	.otherwise({redirectTo: '/search/'});
	});

app.run(function($rootScope, $location){
	$rootScope.searchRepository = function() {
		var keyword = $rootScope.q;
		$location.path('/search/'+ keyword);
	}
});
