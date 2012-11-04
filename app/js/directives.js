angular.module('AwesomeChartJS', []).directive('awesomechart', function () {
        return {
            restrict:'E',
            replace:true,
            template:'<canvas>Your web-browser does not support the HTML 5 canvas element.</canvas>',
            link:function (scope, element, attrs) {
                var chart = new AwesomeChart(attrs.id);
                chart.chartType = attrs.type || 'default';
                chart.title = attrs.title;

                var redraw = function (newValue, oldValue, scope) {
                    //clear it up first: not the nicest method (should be a call on AwesomeChart) but no other choice here...
                    chart.ctx.clearRect(0, 0, chart.width, chart.height);

                    var data = [], labels = [], colors = [];
                    for (var j=0; j<newValue.length; j++){
                        if(newValue && newValue[j] > 0){
                            data.push(newValue[j]);
                            labels.push(scope.labels[j]);
                            colors.push(scope.colors[j%10]);
                        }
                    }
                    chart.data = data;
                    chart.labels = labels;
                    chart.colors = colors;
                    chart.draw();
                };

                scope.$watch(attrs.data, redraw, true);
            }
        }
    }
);