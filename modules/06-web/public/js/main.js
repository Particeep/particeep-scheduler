
function decorate_url(url, params) {
  if (url.indexOf('?') == -1) {
    return `${url}?${params}`;
  } else {
    return `${url}&${params}`;
  }
}

function validate_result(result) {
  if (result.data && result.total_size) {
    return result;
  } else {
    console.error("Result is not the right format. We expect a SearchWithTotalSize and we get", result);
    return ({ data: [], total_size: 0 });
  }
}

function init_search(
  div_id,
  base_url,
  base_columns,
  base_limit,
  base_order_by,
  base_sort_by,
  render_data,
  height
) {
  var base_height = height ? height : '90vh';
  return new gridjs.Grid({
    columns: base_columns,
    search: {
      server: {
        url: (prev, keyword) => decorate_url(prev, `global_search=${keyword}`)
      }
    },
    sort: {
      multiColumn: false,
      server: {
        url: (prev, columns) => {
          if (!columns.length) {
            return decorate_url(prev, `order_by=${base_order_by}&sort_by=${base_sort_by}`);
          }
          const col = columns[0];
          const order_by = col.direction === 1 ? 'asc' : 'desc';
          let sort_by = base_columns[col.index];

          return decorate_url(prev, `order_by=${order_by}&sort_by=${sort_by}`);
        }
      }
    },
    pagination: {
      limit: base_limit,
      server: {
        url: (prev, page, limit) => `${prev}&limit=${limit}&offset=${page * limit}`
      }
    },
    fixedHeader: true,
    height: base_height,
    server: {
      url: base_url,
      then: data => validate_result(data).data.map(render_data),
      total: data => validate_result(data).total_size
    }
  }).render(document.getElementById(div_id));
}
