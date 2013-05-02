'use strict';

/* Filters */
angular.module('zenithubFilters', [])
    .filter('checkmark', function() {
        return function(input) { return input ? '\u2713' : '\u2718';};
    })
    .filter('i18n', function($rootScope) {
        return function(key) {
            return $rootScope.messages[key];
        };
    })
    .filter('date', function() {
        return function(date) {
            var date = new Date(Date.parse(date));
            return date.toLocaleDateString();
        };
    })
    .filter('urlEncode', function(){
        return function(string){
            return encodeURIComponent(string);
        }
    });
