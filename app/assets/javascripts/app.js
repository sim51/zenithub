'use strict';

/* List all necessary angular module for the application */
var app = angular.module('application', ['ngCookies', 'zenithub', 'github', 'play', 'AwesomeChartJS', 'ngCookies']);

/* Configure the application with the route */
app.config(function($routeProvider) {
		$routeProvider
			.when('/error', {templateUrl: '/assets/partials/error.html', controller: ErrorCtrl})
            .when('/me', {templateUrl: '/assets/partials/user.html', controller: MyProfileCtrl})
	      	.when('/repo/:owner/:repository', {templateUrl: '/assets/partials/repository.html', controller: RepositoryHomeCtrl})
	      	.when('/repo/:owner/:repository/members', {templateUrl: '/assets/partials/members.html', controller: RepositoryMembersCtrl})
	      	.when('/repo/:owner/:repository/commits', {templateUrl: '/assets/partials/commits.html', controller: RepositoryCommitsCtrl})
	      	.when('/repo/:owner/:repository/stats/geo', {templateUrl: '/assets/partials/map.html', controller: RepositoryStatsGeoCtrl})
	      	.when('/repo/:owner/:repository/stats/commit', {templateUrl: '/assets/partials/stats_commit.html', controller: RepositoryStatsCommitCtrl})
	      	.when('/search/:keyword', {templateUrl: '/assets/partials/search.html',   controller: RepositoryListCtrl})
	      	.otherwise({redirectTo: '/search/'});
	});

/* What we do when application start ? */
app.run(function($rootScope, $cookieStore, $cookies, $location, Github, Play){
    console.log("[MAIN] Application start");

    // loading i18n properties
    $rootScope.messages = [];
    Play.messages().then(function(data){
        $rootScope.messages = data;
    });

    // we look at cookies to know user is logged
    if ( $cookies['token'] ) {
        $rootScope.isConnected=true;
        $rootScope.token=$cookieStore.get('token').replace(/"/g,'');
        console.log("[MAIN] user is authenticate with token: " + $rootScope.token);
    }

    // search repo function
	$rootScope.searchRepository = function() {
		var keyword = $rootScope.keyword;
		$location.path('/search/'+ keyword);
	}

    // popup for member
	$rootScope.popupMember = function (login, commits, additions, deletions, index){
		Github.user(login).then(function(response){
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
