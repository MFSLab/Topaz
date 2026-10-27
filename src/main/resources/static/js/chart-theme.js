// resources/static/js/chart-theme.js
// Makes every Chart.js chart match the Liquid Glass theme (light or dark).
// Must load AFTER chart.js and BEFORE the inline chart scripts in stats.html.
// Colours come from the CSS variables in css/glass/tokens.css.
(function () {
    if (typeof Chart === 'undefined') return;

    const css = getComputedStyle(document.documentElement);
    const v = (name) => css.getPropertyValue(name).trim();

    const text = v('--chart-text') || '#3A3A3C';
    const grid = v('--chart-grid') || 'rgba(60,60,67,0.14)';

    Chart.defaults.font.family = '"Ubuntu", system-ui, -apple-system, "Segoe UI", sans-serif';
    Chart.defaults.font.size = 12;
    Chart.defaults.color = text;          // ticks, legend, titles
    Chart.defaults.borderColor = grid;    // grid lines & axis borders

    // Legends: round markers instead of boxes
    Chart.defaults.plugins.legend.labels.usePointStyle = true;
    Chart.defaults.plugins.legend.labels.boxWidth = 8;

    // Tooltips: dark frosted pill
    const tip = Chart.defaults.plugins.tooltip;
    tip.backgroundColor = 'rgba(20, 24, 32, 0.82)';
    tip.titleColor = '#FFFFFF';
    tip.bodyColor = '#E5E5EA';
    tip.cornerRadius = 12;
    tip.padding = 10;
    tip.boxPadding = 4;

    // Softer bars
    Chart.defaults.elements.bar.borderRadius = 8;
    Chart.defaults.elements.bar.borderSkipped = false;

    // Short, cheap animation (default is 1s of per-frame redraws on every chart)
    Chart.defaults.animation = { duration: 250, easing: 'easeOutCubic' };

    // LAZY CHARTS: stats.html builds 28 charts up front, but almost all live inside collapsed
    // cards / inactive tabs (display:none). Building a Chart there is wasted work. Charts whose
    // canvas is hidden are created the moment it first becomes visible instead.
    // The inline page scripts are unchanged: they still call `new Chart(ctx, config)`.
    const Native = Chart;
    function LazyChart(target, config) {
        const canvas = target && target.canvas ? target.canvas : target;
        const hidden = canvas && canvas.getClientRects && canvas.getClientRects().length === 0;
        if (!hidden || typeof ResizeObserver === 'undefined') return new Native(target, config);
        const handle = {};
        const ro = new ResizeObserver(function () {
            if (canvas.getClientRects().length > 0) {
                ro.disconnect();
                handle.chart = new Native(target, config);
            }
        });
        ro.observe(canvas);
        return handle;
    }
    Object.setPrototypeOf(LazyChart, Native);      // Chart.defaults / register / getChart still work
    LazyChart.prototype = Native.prototype;
    window.Chart = LazyChart;

    // Exposed for any page script that needs the same palette
    window.TOPAZ_CHART = { text: text, grid: grid };
})();
