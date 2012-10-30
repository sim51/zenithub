'use strict';

/* App Module */

angular.module('zenithub', ['github']).
	config(['$routeProvider', function($routeProvider) {
		$routeProvider
			.when('/error', {templateUrl: 'partials/error.html', controller: ErrorCtrl})
	      	.when('/repo/:owner/:repository', {templateUrl: 'partials/repository.html', controller: RepositoryHomeCtrl})
	      	.when('/repo/:owner/:repository/commits', {templateUrl: 'partials/commits.html', controller: RepositoryCommitsCtrl})
	      	.when('/repo/:owner/:repository/members', {templateUrl: 'partials/members.html', controller: RepositoryMembersCtrl})
	      	.when('/search/:keyword', {templateUrl: 'partials/search.html',   controller: RepositoryListCtrl})
	      	.otherwise({redirectTo: '/search/'});
	}]);
