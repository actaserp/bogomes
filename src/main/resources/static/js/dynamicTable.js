//동적으로 테이블 렌더링
function renderTableWithColumns(targetId, columns, data,
                                enableSelector = true,
                                rowClickHandler = null //행 클릭시 함수 발동할것인가
                                )

{
    const table = document.getElementById(targetId);
    const thead = table.querySelector("thead");
    const tbody = table.querySelector("tbody");

    thead.innerHTML = "";
    tbody.innerHTML = "";

    // ---------------------------
    // HEADER
    // ---------------------------
    let headerHtml = "<tr>";

    if (enableSelector) {
        // 체크박스 컬럼 (좌측 고정)
        headerHtml += `
            <th class="selector-col" style="width:40px; position:sticky; left:0; z-index:20;">
                <input type="checkbox" onclick="toggleAllCheckboxes(this)">
            </th>
        `;
    } else {
        // 선택기 비활성 → hidden-col 적용
        headerHtml += `
            <th class="hidden-col selector-col"></th>
        `;
    }

    columns.forEach((col, idx) => {
        const width = col.visible === false ? "0px" : (col.width ? col.width + "px" : "auto");
        const hidden = col.visible === false ? "hidden-col" : "";

        headerHtml += `
        <th class="${hidden}" style="width:${width}; position:sticky; top:0;">
            ${col.visible === false ? "" : col.header}
        </th>`;
    });

    headerHtml += "</tr>";
    thead.innerHTML = headerHtml;

    // ---------------------------
    // BODY
    // ---------------------------
    let bodyHtml = "";
    data.forEach((row, rowIndex) => {
        bodyHtml += `<tr data-row-index="${rowIndex}" data-row='${JSON.stringify(row)}'>`;

        // 선택기 컬럼
        if (enableSelector) {
            bodyHtml += `
                <td class="selector-col" style="width:40px; position:sticky; left:0; background:white; z-index:15;">
                    <input type="checkbox" class="rowChk" data-row-index="${rowIndex}">
                </td>
            `;
        } else {
            bodyHtml += `<td class="hidden-col selector-col"></td>`;
        }

        columns.forEach(col => {
            const width = col.visible === false ? "0px" : (col.width ? col.width + "px" : "auto");
            const hidden = col.visible === false ? "hidden-col" : "";
            const value = row[col.binding] ?? "";

            bodyHtml += `
                <td class="${hidden}" style="width:${width};">
                    ${col.visible === false ? "" : value}
                </td>`;
        });

        bodyHtml += "</tr>";
    });

    tbody.innerHTML = bodyHtml;

    // ---------------------------
    // Row Click Handler 연결
    // ---------------------------
    if (rowClickHandler){
        tbody.querySelectorAll("tr").forEach(tr => {
            tr.addEventListener('click', () => {
                const rowIndex = Number(tr.dataset.rowIndex);
                const rowData = JSON.parse(tr.dataset.row);
                rowClickHandler(rowData, rowIndex, tr);
            })
        })
    }

    // 컬럼 리사이징 활성화
    enableDoubleClickResize(targetId, columns);
}

// 전체선택
function toggleAllCheckboxes(master) {
    const checks = document.querySelectorAll('.rowChk');
    checks.forEach(c => {
        c.checked = master.checked;
    });
}
//선택된 row 데이터 가져오기
function getSelectedRows(tableId) {
    const table = document.getElementById(tableId);
    const selected = [];

    table.querySelectorAll('.rowChk:checked').forEach(chk => {
        const tr = chk.closest("tr");
        const rowData = JSON.parse(tr.dataset.row); // 원본 데이터
        selected.push(rowData);
    });

    return selected;
}

//////// 얘는 클릭하면 width값 늘어나는 함수
function enableDoubleClickResize(tableId, columns) {
    const table = document.getElementById(tableId);
    const ths = table.querySelectorAll("thead th");

    ths.forEach((th) => {
        if (th.classList.contains('hidden-col') || th.classList.contains('selector-col')) return;

        let lastClick = 0;

        th.addEventListener("click", () => {
            const now = Date.now();
            const colIndex = th.cellIndex;

            if (now - lastClick < 300) {
                // 👉 double click: reset to original columns[].width
                resetColumnWidth(table, th, colIndex, columns);
            } else {
                // 👉 single click: expand
                autoExpandColumnWidth(table, th, colIndex);
            }

            lastClick = now;
        });
    });
}

//////// 얘는 클릭하면 width값 늘어나는 함수
function autoExpandColumnWidth(table, th, colIndex) {
    let originalWidth = th.offsetWidth;
    let maxWidth = originalWidth;

    table.querySelectorAll("tbody tr").forEach(tr => {
        const td = tr.cells[colIndex];
        if (!td) return;

        const tmp = document.createElement("span");
        tmp.style.visibility = "hidden";
        tmp.style.whiteSpace = "nowrap";
        tmp.innerText = td.innerText;
        document.body.appendChild(tmp);

        const textWidth = tmp.offsetWidth + 20;
        if (textWidth > maxWidth) maxWidth = textWidth;

        tmp.remove();
    });

    if (maxWidth <= originalWidth) maxWidth = originalWidth + 30;

    applyWidth(table, th, colIndex, maxWidth);
}

//////// 얘는 width 값 동적 적용하는 함수
function applyWidth(table, th, colIndex, width) {
    th.style.width = width + "px";
    th.style.minWidth = width + "px";
    th.style.maxWidth = width + "px";

    table.querySelectorAll("tbody tr").forEach(tr => {
        const td = tr.cells[colIndex];
        if (td) {
            td.style.width = width + "px";
            td.style.minWidth = width + "px";
            td.style.maxWidth = width + "px";
        }
    });
}

//////// 얘는 더블클릭하면 width값 줄어드는 함수
function resetColumnWidth(table, th, colIndex, columns) {
    // 🔥🔥🔥 여기 width 원본 = columns[colIndex-1].width
    const originalWidth = columns[colIndex - 1]?.width;

    // width 지정 없으면 디폴트 100
    const widthToApply = originalWidth ? originalWidth : 100;


    applyWidth(table, th, colIndex, widthToApply);
}
//////// 컬럼 리사이징 기능