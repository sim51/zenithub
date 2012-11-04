'use strict';

var githuburl = 'http://api.zenithub.logisima.com';
var nominatimurl = 'http://nominatim.openstreetmap.org';

/* Services */
angular.module('github', [ ])
    /* Github service*/
    .factory('$github', function($http, $location, $rootScope){
        return {
            search:function(keyword){
                $('#loading').modal('show');
                var url = githuburl + '/legacy/repos/search/' + keyword + '?callback=JSON_CALLBACK';
                return $http.jsonp( url, {cache:true} )
                    .then(function (response){
                        $('#loading').modal('hide');
                        if( response.status == 200 && response.data.meta.status == 200){
                            return response.data.data.repositories;
                        }else{
                        	$rootScope.error = response.data.data.message;
                            $location.path('/error');
                        }
                    });
            },
            repo:function(owner, repo){
                $('#loading').modal('show');
                var url = githuburl + '/repos/' + owner + '/' + repo  + '?callback=JSON_CALLBACK';
                return $http.jsonp( url, {cache:true} )
                    .then(function (response){
                        $('#loading').modal('hide');
                        if( response.status == 200 && response.data.meta.status == 200){
                            return response.data.data;
                        }else{
                        	$rootScope.error = response.data.data.message;
                            $location.path('/error');
                        }
                    });   
            },
            commit:function(owner, repo, sha){
                var url = githuburl + '/repos/' + owner + '/' + repo  + '/commits/' + sha + '?per_page=100&callback=JSON_CALLBACK';
                return $http.jsonp( url, {cache:true} )
                    .then(function (response){
                        if( response.status == 200 && response.data.meta.status == 200){
                            return response.data.data;
                        }else{
                        	$rootScope.error = response.data.data.message;
                            $location.path('/error');
                        }
                    });   
            },
            commits:function(owner, repo, sha){
                $('#loading').modal('show');
                var url = githuburl + '/repos/' + owner + '/' + repo  + '/commits?per_page=100&callback=JSON_CALLBACK';
                return $http.jsonp( url, {cache:true} )
                    .then(function (response){
                        $('#loading').modal('hide');
                        if( response.status == 200 && response.data.meta.status == 200){
                            return response.data.data;
                        }else{
                            $rootScope.error = response.data.data.message;
                            $location.path('/error');
                        }
                    });   
            },
            members:function(owner, repo){
                $('#loading').modal('show');
                var url = githuburl + '/repos/' + owner + '/' + repo  + '/collaborators?callback=JSON_CALLBACK';
                return $http.jsonp( url, {cache:true} )
                    .then(function (response){
                        $('#loading').modal('hide');
                        if( response.status == 200 && response.data.meta.status == 200){
                            return response.data.data;
                        }else{
                        	$rootScope.error = response.data.data.message;
                            $location.path('/error');
                        }
                    });   
            },
            contributors:function(owner, repo){
                $('#loading').modal('show');
                var url = githuburl + '/repos/' + owner + '/' + repo  + '/contributors?callback=JSON_CALLBACK';
                return $http.jsonp( url, {cache:true} )
                    .then(function (response){
                        $('#loading').modal('hide');
                        if( response.status == 200 && response.data.meta.status == 200){
                            return response.data.data;
                        }else{
                        	$rootScope.error = response.data.data.message;
                            $location.path('/error');
                        }
                    });   
            },
            user:function(login){
                var url = githuburl + '/users/' + login  + '?callback=JSON_CALLBACK';
                return $http.jsonp( url, {cache:true} )
                    .then(function (response){
                        if( response.status == 200 && response.data.meta.status == 200){
                            return response.data.data;
                        }else{
                        	$rootScope.error = response.data.data.message;
                            $location.path('/error');
                        }
                    });   
            }
        }
    })
    /* Play service */
    .factory('$play', function($http, $location, $rootScope){
        return{
            stats:function(owner, repo){
                $('#loading').modal('show');
                var url = playurl + '/repo/' + owner + '/' + repo  + '/commits/stats?callback=JSON_CALLBACK';
                return $http.jsonp( url )
                    .then(function (response){
                        if( response.status == 200 ){
                            return response.data;
                        }else{
                        	$rootScope.error = response.data;
                            $location.path('/error');
                        }
                    });   
            }
        }
    })
    /* nominatim service */
    .factory('$nominatim', function($http, $location, $rootScope){
        return{
            locate:function(location){
                var url = nominatimurl + '/search?q=' + location + '&format=json&json_callback=JSON_CALLBACK';
                return $http.jsonp( url )
                    .then(function (response){
                        if( response.status == 200 ){
                            return response.data;
                        }else{
                            $rootScope.error = response.data;
                            $location.path('/error');
                        }
                    });   
            }
        }
    });
