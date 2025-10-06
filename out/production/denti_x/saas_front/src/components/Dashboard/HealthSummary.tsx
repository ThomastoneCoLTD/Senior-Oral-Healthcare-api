import React from "react";

function HealthSummary() {
  return (
    <div style={{ display: "flex", gap: "1rem", marginBottom: "1rem" }}>
      <div
        style={{
          flex: 1,
          backgroundColor: "#fff",
          borderRadius: "8px",
          padding: "1rem",
          textAlign: "center",
        }}
      >
        <h4>건강 구강 비율</h4>
        <p style={{ fontSize: "2rem", fontWeight: "bold" }}>60%</p>
        <small>건강 3회, 양호 1회, 주의 1회</small>
      </div>
      <div
        style={{
          flex: 1,
          backgroundColor: "#fff",
          borderRadius: "8px",
          padding: "1rem",
          textAlign: "center",
        }}
      >
        <h4>양치 수</h4>
        <p style={{ fontSize: "2rem", fontWeight: "bold" }}>176</p>
        <small>일 평균 1.4회</small>
      </div>
    </div>
  );
}

export default HealthSummary;
