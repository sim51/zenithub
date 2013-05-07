'use strict';

angular.module('zenithub', [])
    .value('Config', {
        githuburl : 'https://api.github.com',
        nominatimurl : 'http://nominatim.openstreetmap.org',
        SUCCES : 'TRUE'
    });

/* github Services */
angular.module('github', ['zenithub'])
    /* Github service*/
    .factory('Github', function($http, $location, $rootScope, Config){
        return {
            search:function(keyword){
                var url = Config.githuburl + '/legacy/repos/search/' + keyword + '?callback=JSON_CALLBACK&access_token=' + $rootScope.token;
                return $http.jsonp( url, {cache:true} )
                    .then(function (response){
                        if( response.status == 200 && response.data.meta.status == 200){
                            return response.data.data.repositories;
                        }else{
                        	$rootScope.error = response.data.data.message;
                            $location.path('/error');
                        }
                    });
            },
            repo:function(owner, repo){
                var url = Config.githuburl + '/repos/' + owner + '/' + repo  + '?callback=JSON_CALLBACK&access_token=' + $rootScope.token;
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
            commit:function(owner, repo, sha){
                var url = Config.githuburl + '/repos/' + owner + '/' + repo  + '/commits/' + sha + '?per_page=100&callback=JSON_CALLBACK&access_token=' + $rootScope.token
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
                var url = Config.githuburl + '/repos/' + owner + '/' + repo  + '/commits?per_page=100&callback=JSON_CALLBACK&access_token=' + $rootScope.token
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
            members:function(owner, repo){
                var url = Config.githuburl + '/repos/' + owner + '/' + repo  + '/collaborators?callback=JSON_CALLBACK&access_token=' + $rootScope.token
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
            contributors:function(owner, repo){
                var url = Config.githuburl + '/repos/' + owner + '/' + repo  + '/contributors?callback=JSON_CALLBACK&access_token=' + $rootScope.token
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
            user:function(login){
                var url = Config.githuburl + '/users/' + login  + '?callback=JSON_CALLBACK&access_token=' + $rootScope.token
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
    /* nominatim service */
    .factory('Nominatim', function($http, $location, $rootScope){
        return{
            locate:function(location){
                var url = Config.nominatimurl + '/search?q=' + location + '&format=json&json_callback=JSON_CALLBACK';
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

angular.module('play', [ ])
    .factory('Play', function($http, $location, $rootScope){
        return {
            messages:function(){
                var url = '/api/messages';
                return $http.get( url )
                    .then(function (response){
                        if( response.status == 200 ){
                            return response.data;
                        }else{
                            $rootScope.error = {
                                title:"Error when retriving I18N messages.",
                                message:$rootScope.messages['error.case'] + "Http code (" + url + ") : " + response.status
                            };
                            $location.path('/error');
                        }
                    });
            },
            token:function(){
                var url = '/api/token';
                return $http.get( url ).then(function (response){
                    if( response.status == 200 ){
                        return response.data.replace(/"/g, '');
                    }else{
                        $rootScope.error = {
                            title:"Error when retriving token.",
                            message:$rootScope.messages['error.case'] + "Http code (" + url + ") : " + response.status
                        };
                        $location.path('/error');
                    }
                });
            },
            sendMail:function(name, email, message, token){
                console.log("[Play|sendMail] Name:" + name + ", email:" + email +", message:" + message +", token:"+token);
                var url = '/api/mail';
                var data = "name="+ name +"&email=" + email +"&message=" + message + "&token=" + token;
                $http({
                    method: 'POST',
                    url: url,
                    data: data,
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                })
                    .success(function(data, status, headers, config) {
                        if( status == 200 ){
                            return SUCCESS;
                        }else{
                            $rootScope.error = {
                                title:"Error when retriving token.",
                                message:$rootScope.messages['error.case'] + "Http code (" + url + ") : " + response.status
                            };
                            $location.path('/error');
                        }
                    })
                    .error(function(data, status, headers, config) {
                        $rootScope.error = {
                            title:$rootScope.messages['error.sendMail'],
                            message:$rootScope.messages['error.case'] + "Http code (" + url + ") : " + response.status
                        };
                        $location.path('/error');
                    })
            },
            indexRepo:function(owner, name, token){
                console.log("[Play:indexing repository " + owner + "/" + name)
                var url = '/api/index/repository/' + owner + '/' + name + '/' + token;
                $http({ method: 'GET', url: url });
            },
            indexUser:function(name, token){
                console.log("[Play:indexing user " + name)
                var url = '/api/index/user/' + name;
                $http({ method: 'GET', url: url });
            }
        }
    });