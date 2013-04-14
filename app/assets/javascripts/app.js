'use strict';

/* App Module */
var app = angular.module('zenithub', ['github', 'AwesomeChartJS']);

app.config(function($routeProvider) {
		$routeProvider
			.when('/error', {templateUrl: '/assets/partials/error.html', controller: ErrorCtrl})
	      	.when('/repo/:owner/:repository', {templateUrl: '/assets/partials/repository.html', controller: RepositoryHomeCtrl})
	      	.when('/repo/:owner/:repository/members', {templateUrl: '/assets/partials/members.html', controller: RepositoryMembersCtrl})
	      	.when('/repo/:owner/:repository/commits', {templateUrl: '/assets/partials/commits.html', controller: RepositoryCommitsCtrl})
	      	.when('/repo/:owner/:repository/stats/geo', {templateUrl: '/assets/partials/map.html', controller: RepositoryStatsGeoCtrl})
	      	.when('/repo/:owner/:repository/stats/commit', {templateUrl: '/assets/partials/stats_commit.html', controller: RepositoryStatsCommitCtrl})
	      	.when('/search/:keyword', {templateUrl: '/assets/partials/search.html',   controller: RepositoryListCtrl})
	      	.otherwise({redirectTo: '/search/'});
	});

app.run(function($rootScope, $github, $location){
	$rootScope.searchRepository = function() {
		var keyword = $rootScope.keyword;
		$location.path('/search/'+ keyword);
	}
	$rootScope.formatDate = function( date1 ) {
		var date = new Date(Date.parse( date1 ));
		return date.toDateString();
	}
	$rootScope.popupMember = function (login, commits, additions, deletions, index){
		$github.user(login).then(function(response){
			$rootScope.user = response;
			if(index >= 0){
				$rootScope.commitsTxt = commits[index] + '/' + 100;
				var addTt = 0;
				for (var j=0; j<additions.length; j++){
					addTt += additions[j];
				}
				$rootScope.additionsTxt = Math.round(additions[index]/addTt*100*100)/100 + '% (' + additions[index] + '/' + addTt + ')';
				var delTt = 0
				for (var j=0; j<deletions.length; j++){
					delTt += deletions[j];
				}
				$rootScope.deletionsTxt = Math.round(deletions[index]/delTt*100*100)/100 + '% (' + deletions[index] + '/' + delTt + ')';
			}
			$('#member').modal('show');
		});
	};
});
