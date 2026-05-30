{% macro clean_text(expr) -%}
regexp_replace(
  cast({{ expr }} as text),
  '[' || chr(160) || chr(65279) || chr(8203) || chr(8204) || chr(8205) || ']',
  '',
  'g'
)
{%- endmacro %}
