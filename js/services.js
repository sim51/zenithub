'use strict';

var githuburl = 'https://api.github.com';

/* Services */
angular.module('github', ['ngResource'])
    /* Github service*/
    .factory('$github', function($http, $location){
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
                            $location.path('/error');
                        }
                    });   
            },
            commits:function(owner, repo){
                $('#loading').modal('show');
                var url = githuburl + '/repos/' + owner + '/' + repo  + '/commits?callback=JSON_CALLBACK';
                return $http.jsonp( url, {cache:true} )
                    .then(function (response){
                        $('#loading').modal('hide');
                        if( response.status == 200 && response.data.meta.status == 200){
                            return response.data.data;
                        }else{
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
                            $location.path('/error');
                        }
                    });   
            }
        }
    });
