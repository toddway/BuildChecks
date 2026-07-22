const table = document.getElementById('findings');
if (table) {
  const rows = [...table.tBodies[0].rows];
  const search = document.getElementById('search');
  const severity = document.getElementById('severity');
  const tool = document.getElementById('tool');
  const newonly = document.getElementById('newonly');
  const apply = () => {
    const text = search.value.toLowerCase();
    rows.forEach(row => {
      row.hidden = (text && !row.textContent.toLowerCase().includes(text))
        || (severity.value && row.dataset.severity !== severity.value)
        || (tool.value && row.dataset.tool !== tool.value)
        || (newonly.checked && row.dataset.new !== 'true');
    });
  };
  [search, severity, tool, newonly].forEach(el => el.addEventListener('input', apply));
  apply(); // honor controls rendered pre-checked (new-only defaults on when there's a baseline)
}
document.querySelectorAll('table.sortable').forEach(sortable => {
  [...sortable.tHead.rows[0].cells].forEach((th, index) => {
    th.addEventListener('click', () => {
      const asc = th.dataset.asc !== 'true';
      [...sortable.tHead.rows[0].cells].forEach(cell => delete cell.dataset.asc);
      th.dataset.asc = asc;
      const body = sortable.tBodies[0];
      [...body.rows]
        .sort((a, b) => {
          const x = a.cells[index].textContent.trim(), y = b.cells[index].textContent.trim();
          const nx = parseFloat(x), ny = parseFloat(y);
          const result = !isNaN(nx) && !isNaN(ny) ? nx - ny : x.localeCompare(y);
          return asc ? result : -result;
        })
        .forEach(row => body.appendChild(row));
    });
  });
});
