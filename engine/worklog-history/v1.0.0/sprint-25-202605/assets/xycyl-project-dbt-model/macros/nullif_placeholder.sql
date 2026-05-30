{% macro nullif_placeholder(expr) -%}
(
  case
    when {{ expr }} is null then null
    when upper(btrim({{ clean_text(expr) }}))
      in ('', '/', '-', '--', 'N/A', 'NA', 'NULL',
          '#N/A', '#VALUE!', '#DIV/0!', '#REF!', '#NAME?', '#NULL!', '#NUM!')
      then null
    when btrim({{ clean_text(expr) }}) ~ '^#[A-Z/]+[!?]?$' then null
    else nullif(btrim({{ clean_text(expr) }}), '')
  end
)
{%- endmacro %}
