import React, { useEffect, useRef, useState } from "react";
import { Column, Grid, Select, SelectItem } from "@carbon/react";
import { injectIntl } from "react-intl";
import "../Style.css";
import { getFromOpenElisServer } from "../utils/Utils";

function TestSelectForm(props) {
  const mounted = useRef(false);
  const [tests, setTests] = useState([]);

  const handleChange = (e) => {
    props.value(e.target.value, e.target.selectedOptions[0].text);
  };

  const getTests = (res) => {
    if (mounted.current) {
      setTests(res);
    }
  };

  useEffect(() => {
    mounted.current = true;
    getFromOpenElisServer("/rest/test-list", getTests);
    return () => {
      mounted.current = false;
    };
  }, []);

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={16}>
          <Select
            defaultValue="placeholder-item"
            id="select-1"
            invalidText="This is an invalid error message."
            helperText={props.title}
            labelText=""
            onChange={handleChange}
          >
            <SelectItem text="" value="" />
            {tests.map((item, idx) => {
              return <SelectItem key={idx} text={item.value} value={item.id} />;
            })}
          </Select>
        </Column>
      </Grid>
    </>
  );
}

export default injectIntl(TestSelectForm);